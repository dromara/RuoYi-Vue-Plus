package com.wudgaby.ticket.domain;

import java.time.Instant;

public record TicketRoutingResult(
    String ticketId,
    Instant processedAt,
    TicketRoutingDecision decision
) {
}
