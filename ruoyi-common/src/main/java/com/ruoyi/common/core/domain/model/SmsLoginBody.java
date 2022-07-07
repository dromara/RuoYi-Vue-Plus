package com.ruoyi.common.core.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 短信登录对象
 *
 * @author Lion Li
 */

@Data
@Schema(name = "短信登录对象")
public class SmsLoginBody {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.phonenumber.not.blank}")
    @Schema(name = "用户手机号")
    private String phonenumber;

    /**
     * 用户密码
     */
    @NotBlank(message = "{sms.code.not.blank}")
    @Schema(name = "短信验证码")
    private String smsCode;

}
