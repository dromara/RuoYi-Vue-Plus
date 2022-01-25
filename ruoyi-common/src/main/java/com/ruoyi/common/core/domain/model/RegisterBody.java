package com.ruoyi.common.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("用户注册对象")
public class RegisterBody extends LoginBody {

    @ApiModelProperty(value = "用户类型")
    private String userType;

}
