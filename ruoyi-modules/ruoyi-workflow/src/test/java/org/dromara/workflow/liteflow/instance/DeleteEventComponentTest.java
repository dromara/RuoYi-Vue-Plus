package org.dromara.workflow.liteflow.instance;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.context.InstanceDeleteContext;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteEventComponentTest {
    private final DefService definitions = mock(DefService.class);
    private final FlowProcessEventHandler events = mock(FlowProcessEventHandler.class);
    private final InstanceDeleteEventComponent component = spy(new InstanceDeleteEventComponent(definitions, events));
    private final InstanceDeleteContext context = new InstanceDeleteContext(null, List.of(1L, 2L), false);

    DeleteEventComponentTest() {
        FlowInstance first = new FlowInstance();
        first.setCreateBy("10");
        first.setBusinessId("A");
        first.setDefinitionId(7L);
        FlowInstance second = new FlowInstance();
        second.setCreateBy("20");
        second.setBusinessId("B");
        second.setDefinitionId(8L);
        context.setFlowInstances(List.of(first, second));
        doReturn(context).when(component).getContextBean(InstanceDeleteContext.class);
    }

    @Test
    void mixedOwnershipRejectsWholeBatchBeforeFirstEvent() {
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("10");
            assertThrows(ServiceException.class, component::process);
            verifyNoInteractions(events, definitions);
        }
    }

    @Test
    void eachBusinessIdUsesItsOwnDefinitionEvenWhenReturnedInReverseOrder() {
        var first = mock(Definition.class);
        var second = mock(Definition.class);
        when(first.getId()).thenReturn(7L);
        when(first.getFlowCode()).thenReturn("payment");
        when(second.getId()).thenReturn(8L);
        when(second.getFlowCode()).thenReturn("leave");
        when(definitions.getByIds(any())).thenReturn(List.of(second, first));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            component.process();
            verify(events).processDeleteHandler("payment", "A");
            verify(events).processDeleteHandler("leave", "B");
            verifyNoMoreInteractions(events);
        }
    }

    @Test
    void adminPublishesOnlyForResolvedDefinitions() {
        Definition definition = mock(Definition.class);
        when(definition.getId()).thenReturn(8L);
        when(definition.getFlowCode()).thenReturn("leave");
        when(definitions.getByIds(any())).thenReturn(List.of(definition));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            component.process();
            verify(events).processDeleteHandler("leave", "B");
            verifyNoMoreInteractions(events);
        }
    }
}
