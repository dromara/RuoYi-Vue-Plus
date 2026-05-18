package com.wudgaby.ticket.application;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AiDecisionValidator {

    private final Validator validator;

    public AiDecisionValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T target) {
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
            throw new IllegalStateException("AI structured output validation failed: " + message);
        }
        return target;
    }
}
