package com.ruoyi.common.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 短信登录对象
 *
 * @author Lion Li
 */

@Data
@ApiModel("短信登录对象")
public class SmsLoginBody {

    /**
     * 用户名
     */
    @NotBlank(message = "{user.phonenumber.not.blank}")
    @ApiModelProperty(value = "用户手机号")
    private String phonenumber;

    /**
     * 用户密码
     */
    @NotBlank(message = "{sms.code.not.blank}")
    @ApiModelProperty(value = "短信验证码")
    private String smsCode;

}
