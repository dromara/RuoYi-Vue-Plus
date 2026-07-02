package org.dromara.common.liteflow.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

/**
 * LiteFlow 恒为 true 的条件节点。
 *
 * @author Lion Li
 */
@LiteflowComponent("alwaysTrue")
public class AlwaysTrueComponent extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        return true;
    }

}
