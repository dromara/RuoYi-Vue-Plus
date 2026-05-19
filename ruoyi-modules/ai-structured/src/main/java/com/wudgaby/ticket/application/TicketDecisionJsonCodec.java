package com.wudgaby.ticket.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wudgaby.ticket.domain.TicketRoutingDecision;
import org.springframework.stereotype.Component;

@Component
public class TicketDecisionJsonCodec {

    private final ObjectMapper objectMapper;

    public TicketDecisionJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(TicketRoutingDecision decision) {
        try {
            return objectMapper.writeValueAsString(decision);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize decision", e);
        }
    }

    public TicketRoutingDecision read(String value) {
        try {
            return objectMapper.readValue(value, TicketRoutingDecision.class);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize decision", e);
        }
    }
}
