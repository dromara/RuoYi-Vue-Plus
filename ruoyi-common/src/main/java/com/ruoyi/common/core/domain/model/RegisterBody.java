package com.ruoyi.common.core.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "用户注册对象")
public class RegisterBody extends LoginBody {

    @Schema(name = "用户类型")
    private String userType;

}
