package org.dromara.common.liteflow;

import cn.hutool.extra.spring.SpringUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.liteflow.utils.LiteFlowUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("LiteFlow 工具契约单元测试")
class LiteFlowUtilsTest {

    private static FlowExecutor flowExecutor;

    /**
     * 注册 LiteFlow 工具所需的 mock 执行器，验证工具逻辑时不启动真实流程引擎。
     */
    @BeforeAll
    static void initializeFlowExecutor() {
        flowExecutor = mock(FlowExecutor.class);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("flowExecutor", flowExecutor);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 清理执行器调用记录，确保每个用例只验证自身的链路交互。
     */
    @BeforeEach
    void resetExecutorInteractions() {
        reset(flowExecutor);
    }

    /**
     * 验证空上下文在调用引擎前被拒绝，并返回统一业务异常。
     */
    @Test
    @DisplayName("拒绝空流程上下文")
    void shouldRejectNullContextBeforeCallingExecutor() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> LiteFlowUtils.execute("demo-chain", null));

        assertEquals("LiteFlow 上下文不能为空", exception.getMessage());
        verifyNoInteractions(flowExecutor);
    }

    /**
     * 验证成功响应只透传链路标识、上下文和空配置，不额外抛出异常。
     */
    @Test
    @DisplayName("执行成功链路")
    void shouldExecuteSuccessfulChain() {
        Object context = new Object();
        LiteflowResponse response = new LiteflowResponse();
        response.setSuccess(true);
        when(flowExecutor.execute2Resp(eq("demo-chain"), isNull(), any(Object.class))).thenReturn(response);

        assertDoesNotThrow(() -> LiteFlowUtils.execute("demo-chain", context));
        verify(flowExecutor).execute2Resp("demo-chain", null, context);
    }

    /**
     * 验证 LiteFlow 返回运行时失败原因时原样抛出，保留业务异常类型和堆栈。
     */
    @Test
    @DisplayName("原样传播运行时失败")
    void shouldRethrowRuntimeFailureCause() {
        IllegalStateException cause = new IllegalStateException("chain failed");
        LiteflowResponse response = failedResponse(cause, "failed");
        Object context = new Object();
        when(flowExecutor.execute2Resp(eq("runtime-chain"), isNull(), any(Object.class))).thenReturn(response);

        assertSame(cause, assertThrows(IllegalStateException.class,
            () -> LiteFlowUtils.execute("runtime-chain", context)));
    }

    /**
     * 验证受检异常和无原因失败会转换为 ServiceException，并分别使用原因或响应消息。
     */
    @Test
    @DisplayName("转换受检异常和无原因失败")
    void shouldWrapCheckedOrMissingFailureCause() {
        Exception checked = new Exception("checked failure");
        Object context = new Object();
        LiteflowResponse checkedResponse = failedResponse(checked, "ignored");
        LiteflowResponse emptyResponse = failedResponse(null, "response failure");
        when(flowExecutor.execute2Resp(eq("checked-chain"), isNull(), any(Object.class)))
            .thenReturn(checkedResponse);
        when(flowExecutor.execute2Resp(eq("empty-chain"), isNull(), any(Object.class)))
            .thenReturn(emptyResponse);

        ServiceException checkedException = assertThrows(ServiceException.class,
            () -> LiteFlowUtils.execute("checked-chain", context));
        ServiceException emptyException = assertThrows(ServiceException.class,
            () -> LiteFlowUtils.execute("empty-chain", context));

        assertEquals("checked failure", checkedException.getMessage());
        assertEquals("response failure", emptyException.getMessage());
    }

    /**
     * 创建失败响应并填充工具日志所需的最小消息字段。
     *
     * @param cause   流程失败原因
     * @param message 流程失败消息
     * @return 失败响应
     */
    private static LiteflowResponse failedResponse(Exception cause, String message) {
        LiteflowResponse response = mock(LiteflowResponse.class);
        when(response.isSuccess()).thenReturn(false);
        when(response.getCause()).thenReturn(cause);
        when(response.getMessage()).thenReturn(message);
        when(response.getRequestId()).thenReturn("request-id");
        when(response.getExecuteStepStrWithTime()).thenReturn("steps");
        return response;
    }
}
