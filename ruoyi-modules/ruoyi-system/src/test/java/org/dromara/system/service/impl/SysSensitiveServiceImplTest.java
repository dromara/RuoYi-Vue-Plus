package org.dromara.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SysSensitiveServiceImplTest {
    @ParameterizedTest
    @CsvSource({
        "false,false,true,true,true", "true,false,true,true,false", "true,false,true,false,true",
        "true,false,false,true,true", "true,true,false,false,false"
    })
    void checksSessionRoleAndPermission(boolean loggedIn, boolean admin, boolean hasRole, boolean hasPermission, boolean mask) {
        String[] roles = {"reviewer"};
        String[] perms = {"record:read"};
        try (var login = mockStatic(LoginHelper.class); var auth = mockStatic(StpUtil.class)) {
            login.when(LoginHelper::isLogin).thenReturn(loggedIn);
            login.when(LoginHelper::isSuperAdmin).thenReturn(admin);
            auth.when(() -> StpUtil.hasRoleOr(roles)).thenReturn(hasRole);
            auth.when(() -> StpUtil.hasPermissionOr(perms)).thenReturn(hasPermission);
            assertEquals(mask, new SysSensitiveServiceImpl().isSensitive(roles, perms));
        }
    }
}
