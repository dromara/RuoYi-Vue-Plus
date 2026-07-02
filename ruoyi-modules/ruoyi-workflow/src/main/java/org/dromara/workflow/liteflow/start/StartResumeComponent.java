package org.dromara.workflow.liteflow.start;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.context.StartProcessContext;
import org.dromara.workflow.mapper.FlwInstanceBizExtMapper;

import java.util.List;

/**
 * 已存在流程实例时执行续提交。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("startResume")
public class StartResumeComponent extends NodeComponent {

    private final TaskService taskService;
    private final InsService insService;
    private final FlwInstanceBizExtMapper flwInstanceBizExtMapper;

    @Override
    public void process() {
        StartProcessContext context = getRequestData();
        BusinessStatusEnum.checkStartStatus(context.getExistingInstance().getFlowStatus());
        List<Task> taskList = taskService.list(new FlowTask().setInstanceId(context.getExistingInstance().getId()));
        if (CollUtil.isEmpty(taskList)) {
            throw new ServiceException("流程实例缺少任务，请检查流程定义配置");
        }
        taskService.mergeVariable(context.getExistingInstance(), context.getVariables());
        insService.updateById(context.getExistingInstance());
        saveBizExt(context);
        context.setStartProcessReturn(new StartProcessReturnDTO(taskList.getFirst().getInstanceId(), taskList.getFirst().getId()));
    }

    private void saveBizExt(StartProcessContext context) {
        FlowInstanceBizExt bizExt = context.getBizExt();
        bizExt.setInstanceId(context.getExistingInstance().getId());
        bizExt.setBusinessId(context.getExistingInstance().getBusinessId());
        flwInstanceBizExtMapper.saveOrUpdateByInstanceId(bizExt);
    }

}
