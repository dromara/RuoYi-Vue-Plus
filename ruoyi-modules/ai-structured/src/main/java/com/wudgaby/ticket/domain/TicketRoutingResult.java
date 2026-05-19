package com.wudgaby.ticket.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TicketRoutingResult(
    @NotBlank String ticketId,
    @NotNull Instant processedAt,
    @NotNull @Valid TicketRoutingDecision decision
) {
}
