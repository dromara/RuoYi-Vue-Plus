package org.dromara.common.ai.handler;

import com.aizuda.snail.ai.agent.chat.starter.SnailAiChatGatewayController;
import com.aizuda.snail.ai.common.execption.BaseSnailAiException;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Snail AI Chat 网关异常处理器。
 * <p>
 * /api/snail/chat/** 接口由 Snail AI SDK 前端消费，响应结构需要保持 SDK Result(status/message/data)，
 * 不能落到 RuoYi 通用 R(code/msg/data) 格式。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SnailAiChatGatewayController.class)
public class SnailAiChatExceptionHandler {

    private static final int AUTHENTICATION_ERROR_STATUS = 5001;

    @ExceptionHandler(SnailAiAuthenticationException.class)
    public Result<Void> handleAuthenticationException(SnailAiAuthenticationException e) {
        log.warn("Snail AI Chat authentication failed: {}", e.getMessage());
        return Result.fail(AUTHENTICATION_ERROR_STATUS, defaultMessage(e, "认证失败，请重新登录"));
    }

    @ExceptionHandler(BaseSnailAiException.class)
    public Result<Void> handleSnailAiException(BaseSnailAiException e) {
        log.warn("Snail AI Chat request failed: {}", e.getMessage());
        return Result.fail(defaultMessage(e, "AI 服务请求失败，请稍后再试"));
    }

    @ExceptionHandler(ModelCallException.class)
    public Result<Void> handleModelCallException(ModelCallException e) {
        log.warn("Snail AI Chat model call failed: {}", e.getMessage(), e);
        return Result.fail(defaultMessage(e, "模型调用失败，请稍后再试"));
    }

    @ExceptionHandler({
        BindException.class,
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class
    })
    public Result<Void> handleValidationException(Exception e) {
        log.warn("Snail AI Chat validation failed: {}", e.getMessage());
        return Result.fail(validationMessage(e));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Snail AI Chat constraint validation failed: {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .filter(this::hasText)
            .collect(Collectors.joining(", "));
        return Result.fail(hasText(message) ? message : "请求参数校验失败");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Snail AI Chat request body parse failed: {}", e.getMessage());
        return Result.fail("请求参数格式错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Snail AI Chat illegal argument: {}", e.getMessage());
        return Result.fail(defaultMessage(e, "请求参数不合法"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("Snail AI Chat illegal state: {}", e.getMessage());
        return Result.fail(defaultMessage(e, "AI 会话状态异常，请刷新后重试"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Snail AI Chat unexpected exception", e);
        return Result.fail("AI 服务异常，请稍后再试");
    }

    private String validationMessage(Exception e) {
        Collection<? extends MessageSourceResolvable> errors;
        if (e instanceof BindException bindException) {
            errors = bindException.getAllErrors();
        } else if (e instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            errors = methodArgumentNotValidException.getBindingResult().getAllErrors();
        } else if (e instanceof HandlerMethodValidationException handlerMethodValidationException) {
            errors = handlerMethodValidationException.getAllErrors();
        } else {
            return "请求参数校验失败";
        }
        String message = errors.stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(this::hasText)
            .collect(Collectors.joining(", "));
        return hasText(message) ? message : "请求参数校验失败";
    }

    private String defaultMessage(Throwable e, String fallback) {
        return hasText(e.getMessage()) ? e.getMessage() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
