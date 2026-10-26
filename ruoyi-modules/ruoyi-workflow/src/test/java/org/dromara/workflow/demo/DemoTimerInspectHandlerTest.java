package org.dromara.workflow.demo;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoTimerInspectHandlerTest {

    @Test
    void timerStartUsesNumericBusinessId() {
        EventWait wait = mock(EventWait.class);
        when(wait.getEventType()).thenReturn(EventType.TIMER_START.getValue());
        when(wait.getDefinitionId()).thenReturn(12L);
        when(wait.getNodeCode()).thenReturn("timerStart");
        Definition definition = mock(Definition.class);
        when(definition.getFlowCode()).thenReturn("demo_timer_start");
        DefService defService = mock(DefService.class);
        when(defService.getById(12L)).thenReturn(definition);
        InsService insService = mock(InsService.class);

        try (MockedStatic<FlowEngine> engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::defService).thenReturn(defService);
            engine.when(FlowEngine::insService).thenReturn(insService);

            assertDoesNotThrow(() -> new DemoTimerInspectHandler().handle(wait));

            ArgumentCaptor<String> businessId = ArgumentCaptor.forClass(String.class);
            verify(insService).startByTimer(businessId.capture(), eq(12L), eq("timerStart"), isNull(), isNull());
            assertTrue(businessId.getValue().matches("[0-9]+"));
            assertTrue(Long.parseLong(businessId.getValue()) > 0);
        }
    }
}
