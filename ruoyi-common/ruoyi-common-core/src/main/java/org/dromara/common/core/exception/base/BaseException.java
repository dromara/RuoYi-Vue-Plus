package org.dromara.common.core.exception.base;

import lombok.AllArgsConstructor;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 基础国际化异常，支持按错误码解析最终提示信息。
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属模块
     */
    private String module;

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误码对应的参数
     */
    private Object[] args;

    /**
     * 错误消息
     */
    private String defaultMessage;

    /**
     * 使用模块、错误码和参数构造异常。
     *
     * @param module 所属模块
     * @param code 错误码
     * @param args 参数
     */
    public BaseException(String module, String code, Object[] args) {
        this(module, code, args, null);
    }

    /**
     * 使用模块和默认消息构造异常。
     *
     * @param module 所属模块
     * @param defaultMessage 默认消息
     */
    public BaseException(String module, String defaultMessage) {
        this(module, null, null, defaultMessage);
    }

    /**
     * 使用错误码和参数构造异常。
     *
     * @param code 错误码
     * @param args 参数
     */
    public BaseException(String code, Object[] args) {
        this(null, code, args, null);
    }

    /**
     * 使用默认消息构造异常。
     *
     * @param defaultMessage 默认消息
     */
    public BaseException(String defaultMessage) {
        this(null, null, null, defaultMessage);
    }

    @Override
    public String getMessage() {
        String message = null;
        if (!StringUtils.isEmpty(code)) {
            message = MessageUtils.message(code, args);
        }
        if (message == null) {
            message = defaultMessage;
        }
        return message;
    }

}
