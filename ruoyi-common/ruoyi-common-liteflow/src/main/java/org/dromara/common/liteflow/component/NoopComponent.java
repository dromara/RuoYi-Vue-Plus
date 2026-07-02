package org.dromara.common.liteflow.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 空节点，用于显式表达无需处理的分支。
 *
 * @author Lion Li
 */
@LiteflowComponent("noop")
public class NoopComponent extends NodeComponent {

    @Override
    public void process() {
        // no-op
    }

}
