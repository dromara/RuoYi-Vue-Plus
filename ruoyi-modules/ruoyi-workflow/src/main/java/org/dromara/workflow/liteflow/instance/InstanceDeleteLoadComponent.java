package org.dromara.workflow.liteflow.instance;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.context.InstanceDeleteContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载待删除流程实例。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Slf4j
@LiteflowComponent("instanceDeleteLoad")
public class InstanceDeleteLoadComponent extends NodeComponent {

    private final FlowInstanceMapper flowInstanceMapper;

    @Override
    public void process() {
        InstanceDeleteContext context = getRequestData();
        List<FlowInstance> flowInstances;
        if (CollUtil.isNotEmpty(context.getBusinessIds())) {
            flowInstances = flowInstanceMapper.selectList(QueryBuilder.lambda(FlowInstance.class)
                .in(FlowInstance::getBusinessId, context.getBusinessIds())
                .build());
            context.setDeleteInstanceIds(StreamUtils.toList(flowInstances, FlowInstance::getId));
        } else {
            flowInstances = flowInstanceMapper.selectByIds(context.getInstanceIds());
            context.setDeleteInstanceIds(new ArrayList<>(context.getInstanceIds()));
        }
        if (CollUtil.isEmpty(flowInstances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            context.setResult(false);
            return;
        }
        context.setFlowInstances(flowInstances);
    }

}
