package org.dromara.web.domain.vo;

import lombok.Data;

/**
 * 登录页租户信息返回对象。
 *
 * @author Michelle.Chung
 */
@Data
public class LoginTenantVo {

    /**
     * 租户开关
     */
    private Boolean tenantEnabled;

}
