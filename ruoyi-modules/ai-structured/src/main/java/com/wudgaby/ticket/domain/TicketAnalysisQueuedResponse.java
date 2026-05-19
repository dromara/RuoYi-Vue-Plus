package com.wudgaby.ticket.domain;

public record TicketAnalysisQueuedResponse(
    String ticketId,
    String topic,
    int partition,
    long offset
) {
}
