package org.dromara.workflow.service.impl;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.workflow.WorkflowServiceImpl;
import org.dromara.warm.flow.core.workflow.command.RevokeCommand;
import org.dromara.warm.flow.core.workflow.command.TerminateCommand;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.bo.FlowCancelBo;
import org.dromara.workflow.domain.bo.FlowInvalidBo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstanceLifecycleTest {
    private final DefService definitions = mock(DefService.class);
    private final InsService instances = mock(InsService.class);
    private final FlwInstanceServiceImpl service = spy(new FlwInstanceServiceImpl(instances, definitions, null, null, null, null, null, null));

    @Test
    void cancellationRequiresDefinitionAndInitiator() {
        FlowInstance instance = new FlowInstance();
        instance.setDefinitionId(8L);
        instance.setId(13L);
        instance.setCreateBy("4");
        instance.setFlowStatus(BusinessStatusEnum.WAITING.getStatus());
        doReturn(instance).when(service).selectInstByBusinessId("A");
        var request = new FlowCancelBo("A", "重新填写");
        var workflow = mock(WorkflowServiceImpl.class);
        try (var login = mockStatic(LoginHelper.class); var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            assertThrows(ServiceException.class, () -> service.cancelProcessApply(request));
            when(definitions.getById(8L)).thenReturn(mock(Definition.class));
            login.when(LoginHelper::getUserIdStr).thenReturn("5");
            assertThrows(ServiceException.class, () -> service.cancelProcessApply(request));
            verifyNoInteractions(workflow);
            login.when(LoginHelper::getUserIdStr).thenReturn("4");
            assertTrue(service.cancelProcessApply(request));
            var command = ArgumentCaptor.forClass(RevokeCommand.class);
            verify(workflow).revoke(command.capture());
            assertEquals(13L, command.getValue().getInstanceId());
            assertEquals("重新填写", command.getValue().getMessage());
        }
    }

    @Test
    void finishedInstanceRejectsBothCancellationAndInvalidation() {
        var instance = new FlowInstance();
        instance.setId(13L);
        instance.setDefinitionId(8L);
        instance.setCreateBy("4");
        instance.setFlowStatus(BusinessStatusEnum.FINISH.getStatus());
        doReturn(instance).when(service).selectInstByBusinessId("A");
        when(instances.getById(13L)).thenReturn(instance);
        when(definitions.getById(8L)).thenReturn(mock(Definition.class));
        try (var login = mockStatic(LoginHelper.class); var engine = mockStatic(FlowEngine.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("4");
            assertThrows(ServiceException.class, () -> service.cancelProcessApply(new FlowCancelBo("A", "撤销")));
            assertThrows(ServiceException.class, () -> service.processInvalid(new FlowInvalidBo(13L, "作废")));
            engine.verify(FlowEngine::workflow, never());
        }
    }

    @Test
    void invalidationSendsStatusAndReasonToEngine() {
        FlowInstance instance = new FlowInstance();
        instance.setFlowStatus(BusinessStatusEnum.WAITING.getStatus());
        when(instances.getById(13L)).thenReturn(instance);
        var workflow = mock(WorkflowServiceImpl.class);
        try (var login = mockStatic(LoginHelper.class); var engine = mockStatic(FlowEngine.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("4");
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            assertTrue(service.processInvalid(new FlowInvalidBo(13L, "业务已取消")));
            var command = ArgumentCaptor.forClass(TerminateCommand.class);
            verify(workflow).terminate(command.capture());
            assertEquals(13L, command.getValue().getInstanceId());
            assertEquals(BusinessStatusEnum.INVALID.getStatus(), command.getValue().getFlowStatus());
            assertEquals(org.dromara.warm.flow.core.enums.TaskStatus.INVALID.getKey(), command.getValue().getTaskStatus());
            assertEquals("业务已取消", command.getValue().getMessage());
        }
    }
}
