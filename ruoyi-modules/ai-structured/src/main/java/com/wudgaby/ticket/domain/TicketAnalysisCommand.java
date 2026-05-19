package com.wudgaby.ticket.domain;

import com.wudgaby.ticket.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketAnalysisCommand(
    @NotBlank String ticketId,
    @NotBlank String userId,
    @NotBlank String content,
    @NotNull ChannelType channelType,
    @NotBlank String locale
) {
}
