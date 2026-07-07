package org.dromara.common.liteflow.utils;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;

/**
 * LiteFlow 执行工具。
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LiteFlowUtils {

    /**
     * 执行 LiteFlow 链路，并按 contextBean 语义透传业务上下文和失败原因。
     *
     * @param chainId 链路标识
     * @param context 链路上下文
     */
    public static void execute(String chainId, Object context) {
        if (context == null) {
            throw new ServiceException("LiteFlow 上下文不能为空");
        }
        LiteflowResponse response = SpringUtils.getBean(FlowExecutor.class).execute2Resp(chainId, null, context);
        if (!response.isSuccess()) {
            Exception cause = response.getCause();
            log.error("LiteFlow 链路执行失败 chainId={} requestId={} message={} steps={}",
                chainId, response.getRequestId(), response.getMessage(), response.getExecuteStepStrWithTime(), cause);
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceException(cause != null ? cause.getMessage() : response.getMessage());
        }
    }

}
