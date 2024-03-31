package org.dromara.workflow.flowable.cmd;

import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cmd.AbstractDynamicInjectionCmd;
import org.flowable.engine.impl.dynamic.BaseDynamicSubProcessInjectUtil;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;

import java.util.List;
import java.util.Map;


public class CustomInjectUserTaskCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    private final FlowElement currentElement;
    private final String processInstanceId;
    private final DynamicUserTaskBuilder dynamicUserTaskBuilder;

    public CustomInjectUserTaskCmd(String processInstanceId, DynamicUserTaskBuilder dynamicUserTaskBuilder, FlowElement currentElement) {
        this.currentElement = currentElement;
        this.processInstanceId = processInstanceId;
        this.dynamicUserTaskBuilder = dynamicUserTaskBuilder;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process, BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        if (!(this.currentElement instanceof UserTask currentUserTask)) {
            return;
        }
        if (currentUserTask.getOutgoingFlows().isEmpty() || currentUserTask.getOutgoingFlows().size() > 1) {
            return;
        }
        SequenceFlow currentOutgoingFlow = currentUserTask.getOutgoingFlows().get(0);
        FlowElement targetFlowElement = currentOutgoingFlow.getTargetFlowElement();
        //创建新的任务节点和两条连线
        UserTask newUserTask = createUserTask(process);
        SequenceFlow newSequenceFlow1 = new SequenceFlow(currentUserTask.getId(), newUserTask.getId());
        newSequenceFlow1.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        SequenceFlow newSequenceFlow2 = new SequenceFlow(newUserTask.getId(), targetFlowElement.getId());
        newSequenceFlow2.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        //添加到流程
        process.addFlowElement(newUserTask);
        process.addFlowElement(newSequenceFlow1);
        process.addFlowElement(newSequenceFlow2);
        process.removeFlowElement(currentOutgoingFlow.getId());
        //获取开始节点
        StartEvent startEvent = process.findFlowElementsOfType(StartEvent.class, false).get(0);
        //绘制新的流程图
        GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(currentUserTask.getId());
        if (elementGraphicInfo != null) {
            double yDiff = 0;
            double xDiff = 80;
            if (elementGraphicInfo.getY() < 173) {
                yDiff = 173 - elementGraphicInfo.getY();
                elementGraphicInfo.setY(173);
            }

            Map<String, GraphicInfo> locationMap = bpmnModel.getLocationMap();
            for (String locationId : locationMap.keySet()) {
                if (startEvent.getId().equals(locationId)) {
                    continue;
                }

                GraphicInfo locationGraphicInfo = locationMap.get(locationId);
                locationGraphicInfo.setX(locationGraphicInfo.getX() + xDiff);
                locationGraphicInfo.setY(locationGraphicInfo.getY() + yDiff);
            }

            Map<String, List<GraphicInfo>> flowLocationMap = bpmnModel.getFlowLocationMap();
            for (String flowId : flowLocationMap.keySet()) {
                List<GraphicInfo> flowGraphicInfoList = flowLocationMap.get(flowId);
                for (GraphicInfo flowGraphicInfo : flowGraphicInfoList) {
                    flowGraphicInfo.setX(flowGraphicInfo.getX() + xDiff);
                    flowGraphicInfo.setY(flowGraphicInfo.getY() + yDiff);
                }
            }
            //移除当前流程连线
            bpmnModel.removeFlowGraphicInfoList(currentOutgoingFlow.getId());
            //重新绘制
            new BpmnAutoLayout(bpmnModel).execute();
        }
        BaseDynamicSubProcessInjectUtil.processFlowElements(commandContext, process, bpmnModel, originalProcessDefinitionEntity, newDeploymentEntity);
    }

    @Override
    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {
    }

    private UserTask createUserTask(Process process) {
        UserTask userTask = new UserTask();
        if (dynamicUserTaskBuilder.getId() != null) {
            userTask.setId(dynamicUserTaskBuilder.getId());
        } else {
            userTask.setId(dynamicUserTaskBuilder.nextTaskId(process.getFlowElementMap()));
        }
        dynamicUserTaskBuilder.setDynamicTaskId(userTask.getId());

        userTask.setName(dynamicUserTaskBuilder.getName());
        userTask.setAssignee(dynamicUserTaskBuilder.getAssignee());
        return userTask;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForProcessInstance(commandContext, processInstanceId);
        return null;
    }
}
