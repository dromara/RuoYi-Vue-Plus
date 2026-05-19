package com.wudgaby.ticket.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wudgaby.ticket.domain.TicketRoutingDecision;
import com.wudgaby.ticket.domain.TicketRoutingResult;
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

    public String write(TicketRoutingResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize routing result", e);
        }
    }

    public TicketRoutingResult readResult(String value) {
        try {
            return objectMapper.readValue(value, TicketRoutingResult.class);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize routing result", e);
        }
    }
}
