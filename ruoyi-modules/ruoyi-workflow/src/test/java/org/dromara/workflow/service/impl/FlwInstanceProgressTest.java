package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.service.IFlwTaskService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlwInstanceProgressTest {

    @Test
    void businessIdResolvesProgressAndMissingInstanceStopsTaskLookup() {
        var instances = mock(FlowInstanceMapper.class);
        var history = mock(FlowHisTaskMapper.class);
        var tasks = mock(IFlwTaskService.class);
        var service = service(instances, history, tasks);
        var instance = new FlowInstance().setBusinessId("business-1").setId(12L);
        when(instances.selectOne(any())).thenReturn(instance, null);

        assertEquals(12L, service.flowHisTaskList("business-1").get("instanceId"));
        assertThrows(ServiceException.class, () -> service.flowHisTaskList("missing"));
        verify(tasks).selectByInstId(12L);
        org.mockito.Mockito.verifyNoMoreInteractions(tasks);
    }

    @Test
    void taskLookupPrefersRunningTaskThenFallsBackToHistory() {
        var instances = mock(FlowInstanceMapper.class);
        var runtime = mock(TaskService.class);
        var history = mock(IFlwTaskService.class);
        var service = new FlwInstanceServiceImpl(null, null, runtime, null, instances, history, null, null);
        var runningInstance = new FlowInstance().setId(12L);
        var finishedInstance = new FlowInstance().setId(14L);
        when(instances.selectById(12L)).thenReturn(runningInstance);
        when(instances.selectById(14L)).thenReturn(finishedInstance);
        when(runtime.getById(3L)).thenReturn(new org.dromara.warm.flow.orm.entity.FlowTask().setInstanceId(12L));
        when(history.selectHisTaskById(5L)).thenReturn(new org.dromara.warm.flow.orm.entity.FlowHisTask().setInstanceId(14L));

        assertEquals(runningInstance, service.selectByTaskId(3L));
        verifyNoInteractions(history);
        assertEquals(finishedInstance, service.selectByTaskId(5L));
        org.junit.jupiter.api.Assertions.assertNull(service.selectByTaskId(99L));
    }

    @Test
    void instanceIdAloneLoadsProgress() {
        FlowInstanceMapper instanceMapper = mock(FlowInstanceMapper.class);
        FlowHisTaskMapper historyMapper = mock(FlowHisTaskMapper.class);
        IFlwTaskService taskService = mock(IFlwTaskService.class);
        FlwInstanceServiceImpl service = service(instanceMapper, historyMapper, taskService);
        FlowInstance instance = mock(FlowInstance.class);
        when(instance.getId()).thenReturn(12L);
        when(instanceMapper.selectById(12L)).thenReturn(instance);
        when(taskService.selectByInstId(12L)).thenReturn(List.of());
        when(historyMapper.selectList(any())).thenReturn(List.of());

        assertEquals(12L, service.flowHisTaskList(12L, null).get("instanceId"));
        verify(instanceMapper).selectById(12L);
    }

    @Test
    void rejectsMismatchedIdsBeforeLoadingTasks() {
        FlowInstanceMapper instanceMapper = mock(FlowInstanceMapper.class);
        FlowHisTaskMapper historyMapper = mock(FlowHisTaskMapper.class);
        IFlwTaskService taskService = mock(IFlwTaskService.class);
        FlwInstanceServiceImpl service = service(instanceMapper, historyMapper, taskService);
        FlowInstance instance = mock(FlowInstance.class);
        when(instance.getBusinessId()).thenReturn("business-1");
        when(instanceMapper.selectById(12L)).thenReturn(instance);

        assertThrows(ServiceException.class, () -> service.flowHisTaskList(12L, "business-2"));
        verifyNoInteractions(taskService, historyMapper);
    }

    @Test
    void matchingIdsLoadSameInstance() {
        FlowInstanceMapper instanceMapper = mock(FlowInstanceMapper.class);
        FlowHisTaskMapper historyMapper = mock(FlowHisTaskMapper.class);
        IFlwTaskService taskService = mock(IFlwTaskService.class);
        FlwInstanceServiceImpl service = service(instanceMapper, historyMapper, taskService);
        FlowInstance instance = mock(FlowInstance.class);
        when(instance.getId()).thenReturn(12L);
        when(instance.getBusinessId()).thenReturn("business-1");
        when(instanceMapper.selectById(12L)).thenReturn(instance);
        when(taskService.selectByInstId(12L)).thenReturn(List.of());
        when(historyMapper.selectList(any())).thenReturn(List.of());

        assertEquals(12L, service.flowHisTaskList(12L, "business-1").get("instanceId"));
        verify(instanceMapper).selectById(12L);
    }

    private FlwInstanceServiceImpl service(FlowInstanceMapper instanceMapper,
                                           FlowHisTaskMapper historyMapper, IFlwTaskService taskService) {
        return new FlwInstanceServiceImpl(mock(InsService.class), mock(DefService.class),
            mock(TaskService.class), historyMapper, instanceMapper, taskService,
            mock(FlwInstanceMapper.class), mock(FlwCategoryMapper.class));
    }
}
