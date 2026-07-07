package org.dromara.common.liteflow.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.liteflow.core.FailMessageProvider;

/**
 * LiteFlow 失败节点，用于显式表达链路分支不可继续执行。
 *
 * @author Lion Li
 */
@LiteflowComponent("fail")
public class FailComponent extends NodeComponent {

    private static final String DEFAULT_MESSAGE = "LiteFlow 链路执行失败";

    @Override
    public void process() {
        throw new ServiceException(getFailMessage());
    }

    /**
     * 优先从上下文读取业务失败提示，未提供时使用默认提示。
     *
     * @return 失败提示
     */
    private String getFailMessage() {
        Object context = getFirstContextBean();
        if (context instanceof FailMessageProvider provider) {
            String message = provider.getFailMessage();
            if (StringUtils.isNotBlank(message)) {
                return message;
            }
        }
        return DEFAULT_MESSAGE;
    }

}
