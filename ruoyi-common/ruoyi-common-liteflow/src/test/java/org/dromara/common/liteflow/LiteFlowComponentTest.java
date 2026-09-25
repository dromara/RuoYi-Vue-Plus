package org.dromara.common.liteflow;

import org.dromara.common.liteflow.component.AlwaysFalseComponent;
import org.dromara.common.liteflow.component.AlwaysTrueComponent;
import org.dromara.common.liteflow.component.NoopComponent;
import org.dromara.common.liteflow.component.FailComponent;
import org.dromara.common.liteflow.component.ContextRequiredComponent;
import org.dromara.common.liteflow.core.FailMessageProvider;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@DisplayName("common-liteflow 功能单元测试")
class LiteFlowComponentTest {

    /**
     * 验证内置 LiteFlow 条件节点始终返回其声明的固定布尔值。
     */
    @Test
    @DisplayName("执行固定布尔条件节点")
    void shouldReturnFixedBooleanValues() {
        assertTrue(new AlwaysTrueComponent().processBoolean());
        assertFalse(new AlwaysFalseComponent().processBoolean());
    }

    /**
     * 验证空操作节点可被安全执行，作为无需业务动作的显式流程分支。
     */
    @Test
    @DisplayName("执行空操作节点")
    void shouldExecuteNoopComponentSafely() {
        assertDoesNotThrow(() -> new NoopComponent().process());
    }

    /**
     * 验证失败节点优先采用业务上下文消息，并在缺少上下文时使用统一默认消息。
     */
    @Test
    @DisplayName("从流程上下文解析失败消息")
    void shouldResolveFailureMessageFromContextOrDefault() {
        FailComponent contextual = spy(new FailComponent());
        FailMessageProvider provider = () -> "库存不足";
        doReturn(provider).when(contextual).getFirstContextBean();
        FailComponent fallback = spy(new FailComponent());
        doReturn(null).when(fallback).getFirstContextBean();

        assertEquals("库存不足", assertThrows(ServiceException.class, contextual::process).getMessage());
        assertEquals("LiteFlow 链路执行失败", assertThrows(ServiceException.class, fallback::process).getMessage());
    }

    /**
     * 验证上下文必填节点拒绝空上下文，并允许有效流程上下文继续执行。
     */
    @Test
    @DisplayName("校验流程上下文是否存在")
    void shouldRequireLiteFlowContext() {
        ContextRequiredComponent missing = spy(new ContextRequiredComponent());
        doReturn(null).when(missing).getFirstContextBean();
        ContextRequiredComponent present = spy(new ContextRequiredComponent());
        doReturn(new Object()).when(present).getFirstContextBean();

        assertEquals("LiteFlow 上下文不能为空", assertThrows(ServiceException.class, missing::process).getMessage());
        assertDoesNotThrow(present::process);
    }
}
