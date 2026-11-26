package org.dromara.workflow.liteflow.start;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.workflow.WorkflowServiceImpl;
import org.dromara.warm.flow.core.workflow.command.StartCommand;
import org.dromara.warm.flow.core.workflow.result.WorkflowResult;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.domain.context.StartProcessContext;
import org.dromara.workflow.mapper.FlwInstanceBizExtMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartExecuteComponentTest {
    @org.junit.jupiter.api.Test
    void missingBusinessIdOrUnpublishedDefinitionStopsBeforeStart() {
        var instances = mock(org.dromara.warm.flow.orm.mapper.FlowInstanceMapper.class);
        var definitions = mock(org.dromara.warm.flow.core.service.DefService.class);
        var context = new StartProcessContext(new StartProcessBo());
        context.getStartProcessBo().setBusinessId(" ");
        context.getStartProcessBo().setFlowCode("leave");
        var prepareRequest = spy(new StartPrepareRequestComponent(instances));
        var prepareInstance = spy(new StartPrepareInstanceComponent());
        doReturn(context).when(prepareRequest).getContextBean(StartProcessContext.class);
        doReturn(context).when(prepareInstance).getContextBean(StartProcessContext.class);
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::defService).thenReturn(definitions);
            assertThrows(ServiceException.class, prepareRequest::process);
            verifyNoInteractions(instances, definitions);
            context.getStartProcessBo().setBusinessId("A");
            assertThrows(ServiceException.class, prepareInstance::process);
            verify(definitions).getPublishByFlowCode("leave");
            engine.verify(FlowEngine::workflow, never());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void startRequiresExactlyOneFirstTask(int count) {
        var workflow = mock(WorkflowServiceImpl.class);
        var instances = mock(InsService.class);
        var tasks = mock(TaskService.class);
        var extensions = mock(FlwInstanceBizExtMapper.class);
        var component = spy(new StartExecuteComponent(tasks, extensions));
        var context = new StartProcessContext(new StartProcessBo());
        context.getStartProcessBo().setFlowCode("leave");
        context.getStartProcessBo().setHandler("7");
        context.setBusinessId("A");
        context.setVariables(Map.of("days", 3));
        context.setBizExt(new FlowInstanceBizExt());
        doReturn(context).when(component).getContextBean(StartProcessContext.class);
        var result = mock(WorkflowResult.class);
        when(result.getInstanceId()).thenReturn(31L);
        when(workflow.start(any())).thenReturn(result);
        var instance = mock(Instance.class);
        when(instances.getById(31L)).thenReturn(instance);
        when(instance.getId()).thenReturn(31L);
        when(instance.getBusinessId()).thenReturn("A");
        var task = mock(Task.class);
        when(task.getId()).thenReturn(50L);
        when(tasks.list(any())).thenReturn(Collections.nCopies(count, task));
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            engine.when(FlowEngine::insService).thenReturn(instances);
            if (count == 1) {
                component.process();
                assertEquals(50L, context.getStartProcessReturn().taskId());
                assertEquals(31L, context.getStartProcessReturn().processInstanceId());
                verify(extensions).saveOrUpdateByInstanceId(context.getBizExt());
                assertEquals(31L, context.getBizExt().getInstanceId());
                assertEquals("A", context.getBizExt().getBusinessId());
            } else {
                var error = assertThrows(ServiceException.class, component::process);
                assertEquals(count == 0 ? "流程启动失败，未生成任务" : "请检查流程第一个环节是否为申请人！", error.getMessage());
                assertNull(context.getStartProcessReturn());
            }
            var start = ArgumentCaptor.forClass(StartCommand.class);
            verify(workflow).start(start.capture());
            assertEquals("A", start.getValue().getBusinessId());
            assertEquals("leave", start.getValue().getFlowCode());
            assertEquals(BusinessStatusEnum.DRAFT.getStatus(), start.getValue().getFlowStatus());
            assertEquals(3, start.getValue().getVariables().get("days"));
        }
    }
}
