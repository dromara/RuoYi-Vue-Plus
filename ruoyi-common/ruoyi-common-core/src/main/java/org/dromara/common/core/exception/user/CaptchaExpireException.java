package org.dromara.common.core.exception.user;

import java.io.Serial;

/**
 * 验证码失效异常类
 *
 * @author ruoyi
 */
public class CaptchaExpireException extends UserException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造验证码失效异常。
     */
    public CaptchaExpireException() {
        super("user.jcaptcha.expire");
    }
}
