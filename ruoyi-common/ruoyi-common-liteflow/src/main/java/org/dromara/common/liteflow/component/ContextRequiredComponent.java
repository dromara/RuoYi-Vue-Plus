package org.dromara.common.liteflow.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 上下文必填校验节点。
 *
 * @author Lion Li
 */
@LiteflowComponent("contextRequired")
public class ContextRequiredComponent extends NodeComponent {

    @Override
    public void process() {
        if (getFirstContextBean() == null) {
            throw new RuntimeException("LiteFlow 上下文不能为空");
        }
    }

}
