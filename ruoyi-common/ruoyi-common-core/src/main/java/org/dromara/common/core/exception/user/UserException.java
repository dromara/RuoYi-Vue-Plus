package org.dromara.common.core.exception.user;

import org.dromara.common.core.exception.base.BaseException;

import java.io.Serial;

/**
 * 用户信息异常类
 *
 * @author ruoyi
 */
public class UserException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造用户模块异常。
     *
     * @param code 错误码
     * @param args 错误码参数
     */
    public UserException(String code, Object... args) {
        super("user", code, args, null);
    }
}
