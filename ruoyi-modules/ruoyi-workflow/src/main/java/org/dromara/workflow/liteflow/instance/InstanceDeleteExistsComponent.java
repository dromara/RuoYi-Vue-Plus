package org.dromara.workflow.liteflow.instance;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.context.InstanceDeleteContext;

/**
 * 判断删除流程实例时是否存在可执行删除的数据。
 *
 * @author may
 */
@ConditionalOnEnable
@LiteflowComponent("instanceDeleteExists")
public class InstanceDeleteExistsComponent extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        InstanceDeleteContext context = getContextBean(InstanceDeleteContext.class);
        return CollUtil.isNotEmpty(context.getFlowInstances());
    }

}
