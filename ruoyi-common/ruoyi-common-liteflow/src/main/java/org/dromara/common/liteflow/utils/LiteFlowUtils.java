package org.dromara.common.liteflow.utils;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;

/**
 * LiteFlow 执行工具。
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LiteFlowUtils {

    /**
     * 执行 LiteFlow 链路，并按业务异常语义透传失败原因。
     *
     * @param chainId 链路标识
     * @param context 链路上下文
     */
    public static void execute(String chainId, Object context) {
        LiteflowResponse response = SpringUtils.getBean(FlowExecutor.class).execute2Resp(chainId, context);
        if (!response.isSuccess()) {
            Exception cause = response.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceException(cause != null ? cause.getMessage() : response.getMessage());
        }
    }

}
