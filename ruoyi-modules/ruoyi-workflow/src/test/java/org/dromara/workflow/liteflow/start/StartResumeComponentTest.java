package org.dromara.workflow.liteflow.start;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.domain.context.StartProcessContext;
import org.dromara.workflow.mapper.FlwInstanceBizExtMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartResumeComponentTest {
    private final TaskService tasks = mock(TaskService.class);
    private final InsService instances = mock(InsService.class);
    private final FlwInstanceBizExtMapper extensions = mock(FlwInstanceBizExtMapper.class);
    private final StartResumeComponent component = spy(new StartResumeComponent(tasks, instances, extensions));
    private final StartProcessContext context = new StartProcessContext(new StartProcessBo());
    private final FlowInstance instance = new FlowInstance();

    StartResumeComponentTest() {
        instance.setId(71L);
        instance.setBusinessId("A-23");
        instance.setFlowStatus(BusinessStatusEnum.DRAFT.getStatus());
        context.setExistingInstance(instance);
        context.setVariables(Map.of("days", 2));
        context.setBizExt(new FlowInstanceBizExt());
        doReturn(context).when(component).getContextBean(StartProcessContext.class);
    }

    @Test
    void resumesOriginalInstanceAndReusesItsTask() {
        Task task = mock(Task.class);
        when(task.getInstanceId()).thenReturn(71L);
        when(task.getId()).thenReturn(83L);
        when(tasks.list(any())).thenReturn(List.of(task));
        component.process();
        verify(tasks).mergeVariable(instance, context.getVariables());
        verify(instances).updateById(instance);
        verify(extensions).saveOrUpdateByInstanceId(context.getBizExt());
        assertEquals("A-23", context.getBizExt().getBusinessId());
        assertEquals(71L, context.getBizExt().getInstanceId());
        assertEquals(83L, context.getStartProcessReturn().taskId());
    }

    @Test
    void missingTaskDoesNotSaveAnything() {
        assertThrows(ServiceException.class, component::process);
        verifyNoInteractions(instances, extensions);
    }

    @Test
    void finishedInstanceCannotResume() {
        instance.setFlowStatus(BusinessStatusEnum.FINISH.getStatus());
        assertThrows(ServiceException.class, component::process);
        verifyNoInteractions(tasks, instances, extensions);
    }
}
