package org.dromara.common.core.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 第三方登录用户身份权限
 *
 * @author thiszhc is 三三
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SocialLogin extends LoginUser{

    /**
     * openid
     */
    private String openid;
}
