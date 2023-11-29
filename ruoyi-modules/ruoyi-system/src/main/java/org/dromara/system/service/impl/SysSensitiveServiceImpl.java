package org.dromara.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.sensitive.core.SensitiveService;
import org.dromara.common.tenant.helper.TenantHelper;
import org.springframework.stereotype.Service;

/**
 * 脱敏服务
 * 默认管理员不过滤
 * 需自行根据业务重写实现
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Service
public class SysSensitiveServiceImpl implements SensitiveService {

    /**
     * 是否脱敏
     */
    @Override
    public boolean isSensitive(String roleKey, String perms) {
        if (!LoginHelper.isLogin()) {
            return true;
        }
        boolean roleExist = StringUtils.isNotBlank(roleKey);
        boolean permsExist = StringUtils.isNotBlank(perms);
        if (roleExist && permsExist) {
            if (StpUtil.hasRole(roleKey) && StpUtil.hasPermission(perms)) {
                return false;
            }
        } else if (roleExist && StpUtil.hasRole(roleKey)) {
            return false;
        } else if (permsExist && StpUtil.hasPermission(perms)) {
            return false;
        }

        if (TenantHelper.isEnable()) {
            return !LoginHelper.isSuperAdmin() && !LoginHelper.isTenantAdmin();
        }
        return !LoginHelper.isSuperAdmin();
    }

}
