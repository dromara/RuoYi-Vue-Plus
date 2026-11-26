package org.dromara.workflow.demo;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.impl.EventRuntimeHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.mockito.Mockito.*;

class TimerFallbackTest {
    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"TIMER_CATCH", "TIMER_START"})
    void delegatesOtherFlowsAndIntermediateTimers(EventType type) {
        var wait = mock(EventWait.class);
        when(wait.getEventType()).thenReturn(type.getValue());
        when(wait.getDefinitionId()).thenReturn(7L);
        var definitions = mock(DefService.class);
        var definition = mock(Definition.class);
        when(definition.getFlowCode()).thenReturn("other");
        when(definitions.getById(7L)).thenReturn(definition);
        try (var engine = mockStatic(FlowEngine.class); var fallback = mockConstruction(EventRuntimeHandler.class)) {
            engine.when(FlowEngine::defService).thenReturn(definitions);
            new DemoTimerInspectHandler().handle(wait);
            verify(fallback.constructed().getFirst()).handle(wait);
            engine.verify(FlowEngine::insService, never());
        }
    }
}
