package org.dromara.workflow.service.impl;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.config.WarmFlow;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.workflow.context.WorkflowContext;
import org.dromara.workflow.api.domain.StartMessageEventDTO;
import org.dromara.workflow.api.domain.StartTimerEventDTO;
import org.dromara.workflow.api.domain.TriggerMessageEventDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowServiceImplEventTest {

    private final WorkflowServiceImpl service = new WorkflowServiceImpl(null, null);

    @Test
    void messageStartPassesPayloadAndHandler() {
        StartMessageEventDTO request = new StartMessageEventDTO();
        request.setBusinessId("order-1");
        request.setFlowCode("order_flow");
        request.setEventName("paid");
        request.setPayload(Map.of("amount", 20));
        request.setHandler("42");
        InsService insService = mock(InsService.class);
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(100L);
        when(insService.startByMessage(eq("order-1"), eq("order_flow"), eq("paid"), eq(request.getPayload()),
            any(WorkflowContext.class))).thenReturn(instance);

        try (MockedStatic<FlowEngine> engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::insService).thenReturn(insService);
            assertEquals(100L, service.startByMessageEvent(request));
            ArgumentCaptor<WorkflowContext> context = ArgumentCaptor.forClass(WorkflowContext.class);
            verify(insService).startByMessage(eq("order-1"), eq("order_flow"), eq("paid"),
                eq(request.getPayload()), context.capture());
            assertEquals("42", context.getValue().getHandler());
        }
    }

    @Test
    void timerStartGeneratesNumericBusinessIdAndUsesSystemHandler() {
        StartTimerEventDTO request = new StartTimerEventDTO();
        request.setDefinitionId(10L);
        request.setNodeCode("timerStart");
        InsService insService = mock(InsService.class);
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(200L);
        when(insService.startByTimer(any(String.class), eq(10L), eq("timerStart"),
            any(), any(WorkflowContext.class))).thenReturn(instance);
        WarmFlow config = mock(WarmFlow.class);
        when(config.getSystemHandler()).thenReturn("system");

        try (MockedStatic<FlowEngine> engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::insService).thenReturn(insService);
            engine.when(FlowEngine::getFlowConfig).thenReturn(config);
            assertEquals(200L, service.startByTimerEvent(request));
            ArgumentCaptor<String> businessId = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<WorkflowContext> context = ArgumentCaptor.forClass(WorkflowContext.class);
            verify(insService).startByTimer(businessId.capture(), eq(10L), eq("timerStart"),
                any(), context.capture());
            assertTrue(businessId.getValue().matches("[0-9]+"));
            assertEquals("system", context.getValue().getHandler());
        }
    }

    @Test
    void timerStartKeepsProvidedBusinessIdAndHandler() {
        StartTimerEventDTO request = new StartTimerEventDTO();
        request.setBusinessId("manual-42");
        request.setDefinitionId(10L);
        request.setNodeCode("timerStart");
        request.setHandler("operator");
        request.setPayload(Map.of("reason", "retry"));
        InsService insService = mock(InsService.class);
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(201L);
        when(insService.startByTimer(eq("manual-42"), eq(10L), eq("timerStart"),
            eq(request.getPayload()), any(WorkflowContext.class))).thenReturn(instance);

        try (MockedStatic<FlowEngine> engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::insService).thenReturn(insService);
            assertEquals(201L, service.startByTimerEvent(request));
            ArgumentCaptor<WorkflowContext> context = ArgumentCaptor.forClass(WorkflowContext.class);
            verify(insService).startByTimer(eq("manual-42"), eq(10L), eq("timerStart"),
                eq(request.getPayload()), context.capture());
            assertEquals("operator", context.getValue().getHandler());
        }
    }

    @Test
    void messageAndErrorEventsDelegateToEngine() {
        TriggerMessageEventDTO request = new TriggerMessageEventDTO();
        request.setInstanceId(300L);
        request.setEventName("confirmed");
        request.setHandler("42");
        InsService insService = mock(InsService.class);
        TaskService taskService = mock(TaskService.class);
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(300L);
        when(insService.triggerMessage(eq(300L), eq("confirmed"), any(), any(WorkflowContext.class)))
            .thenReturn(instance);
        when(insService.throwError(300L, "external_error", "failed")).thenReturn(true);
        when(taskService.throwError(400L, "external_error", "failed")).thenReturn(true);

        try (MockedStatic<FlowEngine> engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::insService).thenReturn(insService);
            engine.when(FlowEngine::taskService).thenReturn(taskService);
            assertEquals(300L, service.triggerMessageEvent(request));
            ArgumentCaptor<WorkflowContext> context = ArgumentCaptor.forClass(WorkflowContext.class);
            verify(insService).triggerMessage(eq(300L), eq("confirmed"), any(), context.capture());
            assertEquals("42", context.getValue().getHandler());
            assertTrue(service.throwInstanceErrorEvent(300L, "external_error", "failed"));
            assertTrue(service.throwTaskErrorEvent(400L, "external_error", "failed"));
        }
    }
}
