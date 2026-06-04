package org.dromara.common.core.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * SSE 场景专用异常。
 *
 * @author LionLi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public final class SseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;

    /**
     * 使用错误消息构造 SSE 异常。
     *
     * @param message 错误消息
     */
    public SseException(String message) {
        this.message = message;
    }

    /**
     * 使用错误消息和错误码构造 SSE 异常。
     *
     * @param message 错误消息
     * @param code    错误码
     */
    public SseException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    /**
     * 获取错误提示。
     *
     * @return 错误提示
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 设置错误消息并返回当前异常对象。
     *
     * @param message 错误消息
     * @return 当前异常对象
     */
    public SseException setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 设置错误明细并返回当前异常对象。
     *
     * @param detailMessage 错误明细
     * @return 当前异常对象
     */
    public SseException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}
