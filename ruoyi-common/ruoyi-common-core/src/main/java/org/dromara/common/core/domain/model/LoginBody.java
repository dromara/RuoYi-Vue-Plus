package org.dromara.common.core.domain.model;

import jakarta.validation.constraints.Email;
import org.dromara.common.core.constant.UserConstants;
import lombok.Data;
import org.dromara.common.core.validate.auth.EmailGroup;
import org.dromara.common.core.validate.auth.PasswordGroup;
import org.dromara.common.core.validate.auth.SmsGroup;
import org.dromara.common.core.validate.auth.WechatGroup;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录对象
 *
 * @author Lion Li
 */

@Data
public class LoginBody {

    /**
     * 客户端id
     */
    @NotBlank(message = "{auth.clientid.not.blank}")
    private String clientId;

    /**
     * 客户端key
     */
    private String clientKey;

    /**
     * 客户端秘钥
     */
    private String clientSecret;

    /**
     * 授权类型
     */
    @NotBlank(message = "{auth.grant.type.not.blank}")
    private String grantType;

    /**
     * 租户ID
     */
    @NotBlank(message = "{tenant.number.not.blank}")
    private String tenantId;

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}", groups = {PasswordGroup.class})
    @Length(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH, message = "{user.username.length.valid}", groups = {PasswordGroup.class})
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}", groups = {PasswordGroup.class})
    @Length(min = UserConstants.PASSWORD_MIN_LENGTH, max = UserConstants.PASSWORD_MAX_LENGTH, message = "{user.password.length.valid}", groups = {PasswordGroup.class})
    private String password;

    /**
     * 验证码
     */
    private String code;

    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * 手机号
     */
    @NotBlank(message = "{user.phonenumber.not.blank}", groups = {SmsGroup.class})
    private String phonenumber;

    /**
     * 短信code
     */
    @NotBlank(message = "{sms.code.not.blank}", groups = {SmsGroup.class})
    private String smsCode;

    /**
     * 邮箱
     */
    @NotBlank(message = "{user.email.not.blank}", groups = {EmailGroup.class})
    @Email(message = "{user.email.not.valid}")
    private String email;

    /**
     * 邮箱code
     */
    @NotBlank(message = "{email.code.not.blank}", groups = {EmailGroup.class})
    private String emailCode;

    /**
     * 小程序code
     */
    @NotBlank(message = "{xcx.code.not.blank}", groups = {WechatGroup.class})
    private String xcxCode;

}
