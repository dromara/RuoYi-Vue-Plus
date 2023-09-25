package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.common.core.validate.auth.PasswordGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户密码修改bo
 */
@Data
public class SysUserPasswordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空", groups = { PasswordGroup.class })
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空", groups = { PasswordGroup.class })
    private String newPassword;
}
