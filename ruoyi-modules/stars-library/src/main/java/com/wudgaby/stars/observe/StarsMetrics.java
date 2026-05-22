package com.wudgaby.stars.observe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Stars 模块 Prometheus 指标（导入任务与 AI enrichment）
 */
@Component
public class StarsMetrics {

    private final Counter importJobStartedTotal;
    private final Counter importJobDoneTotal;
    private final Counter importJobPartialTotal;
    private final Counter importJobFailedTotal;
    private final Timer importJobLatency;

    private final Counter enrichmentRequestTotal;
    private final Counter enrichmentSuccessTotal;
    private final Counter enrichmentFailureTotal;
    private final Timer enrichmentLatency;

    public StarsMetrics(MeterRegistry registry) {
        this.importJobStartedTotal = registry.counter("stars_import_job_started_total");
        this.importJobDoneTotal = registry.counter("stars_import_job_done_total");
        this.importJobPartialTotal = registry.counter("stars_import_job_partial_total");
        this.importJobFailedTotal = registry.counter("stars_import_job_failed_total");
        this.importJobLatency = registry.timer("stars_import_job_latency_ms");

        this.enrichmentRequestTotal = registry.counter("stars_enrichment_request_total");
        this.enrichmentSuccessTotal = registry.counter("stars_enrichment_success_total");
        this.enrichmentFailureTotal = registry.counter("stars_enrichment_failure_total");
        this.enrichmentLatency = registry.timer("stars_enrichment_latency_ms");
    }

    public void incrementImportJobStarted() {
        importJobStartedTotal.increment();
    }

    public void incrementImportJobDone() {
        importJobDoneTotal.increment();
    }

    public void incrementImportJobPartial() {
        importJobPartialTotal.increment();
    }

    public void incrementImportJobFailed() {
        importJobFailedTotal.increment();
    }

    public Timer importJobTimer() {
        return importJobLatency;
    }

    public void incrementEnrichmentRequest() {
        enrichmentRequestTotal.increment();
    }

    public void incrementEnrichmentSuccess() {
        enrichmentSuccessTotal.increment();
    }

    public void incrementEnrichmentFailure() {
        enrichmentFailureTotal.increment();
    }

    public Timer enrichmentTimer() {
        return enrichmentLatency;
    }
}
