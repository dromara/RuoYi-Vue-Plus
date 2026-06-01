package org.dromara.common.core.exception;

import cn.hutool.core.text.StrFormatter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 通用业务异常，支持使用占位符拼接错误信息。
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public final class ServiceException extends RuntimeException {

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
     * 使用错误消息构造业务异常。
     *
     * @param message 错误消息
     */
    public ServiceException(String message) {
        this.message = message;
    }

    /**
     * 使用错误消息和错误码构造业务异常。
     *
     * @param message 错误消息
     * @param code 错误码
     */
    public ServiceException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    /**
     * 使用错误消息和根因构造业务异常。
     *
     * @param message 错误消息
     * @param cause 根因
     */
    public ServiceException(String message, Throwable cause) {
        super(cause);
        this.message = message;
    }

    /**
     * 使用占位符参数格式化错误消息。
     *
     * @param message 模板消息
     * @param args 参数
     */
    public ServiceException(String message, Object... args) {
        this.message = StrFormatter.format(message, args);
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
    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 设置错误明细并返回当前异常对象。
     *
     * @param detailMessage 错误明细
     * @return 当前异常对象
     */
    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}
