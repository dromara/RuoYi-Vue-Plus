package org.dromara.workflow.config;

import org.dromara.workflow.handler.WorkflowEventPublisher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowSwitchTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"true", "false"})
    void registersWorkflowAndPublisherOnlyWhenEnabled(String enabled) {
        try (var context = new AnnotationConfigApplicationContext()) {
            if (enabled != null) {
                context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("workflow-test", Map.of("warm-flow.enabled", enabled)));
            }
            context.register(WarmFlowConfig.class, WorkflowEventPublisher.class);
            context.refresh();
            boolean expected = Boolean.parseBoolean(enabled);
            assertEquals(expected, context.containsBean("warmFlowConfig"));
            assertEquals(expected, context.containsBean("workflowEventPublisher"));
        }
    }
}
