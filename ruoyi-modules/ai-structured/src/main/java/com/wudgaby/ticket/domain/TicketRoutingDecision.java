package com.wudgaby.ticket.domain;

import com.wudgaby.ticket.enums.SentimentLabel;
import com.wudgaby.ticket.enums.TicketIntent;
import com.wudgaby.ticket.enums.TicketPriority;
import jakarta.validation.constraints.*;

import java.util.List;

public record TicketRoutingDecision(
    @NotNull TicketIntent intent,
    @NotNull TicketPriority priority,
    @NotNull SentimentLabel sentiment,
    boolean requiresHuman,
    @NotBlank String queue,
    @Min(0) @Max(100) int confidence,
    @NotEmpty List<@NotBlank String> reasons,
    List<@NotBlank String> riskTags,
    String summary
) {
}
