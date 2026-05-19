package com.wudgaby.ticket.abserve;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AiMetrics {

    private final Counter requestTotal;
    private final Counter parseFailureTotal;
    private final Counter validationFailureTotal;
    private final Counter fallbackTotal;
    private final Timer latencyTimer;

    public AiMetrics(MeterRegistry registry) {
        this.requestTotal = registry.counter("ai_request_total", "scene", "ticket_routing");
        this.parseFailureTotal = registry.counter("ai_structured_parse_failure_total", "scene", "ticket_routing");
        this.validationFailureTotal = registry.counter("ai_validation_failure_total", "scene", "ticket_routing");
        this.fallbackTotal = registry.counter("ai_fallback_total", "scene", "ticket_routing");
        this.latencyTimer = registry.timer("ai_latency_ms", "scene", "ticket_routing");
    }

    public void incrementRequest() {
        requestTotal.increment();
    }

    public void incrementParseFailure() {
        parseFailureTotal.increment();
    }

    public void incrementValidationFailure() {
        validationFailureTotal.increment();
    }

    public void incrementFallback() {
        fallbackTotal.increment();
    }

    public Timer timer() {
        return latencyTimer;
    }
}
