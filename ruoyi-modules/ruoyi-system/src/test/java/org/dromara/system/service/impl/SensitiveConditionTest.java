package org.dromara.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SensitiveConditionTest {

    @ParameterizedTest
    @CsvSource({"role,true,false","role,false,true","permission,true,false","permission,false,true","empty,false,true"})
    void evaluatesSingleConfiguredCondition(String condition, boolean matches, boolean masked) {
        String[] roles = condition.equals("role") ? new String[]{"buyer"} : new String[0];
        String[] permissions = condition.equals("permission") ? new String[]{"orders:view"} : new String[0];
        try (var login = mockStatic(LoginHelper.class); var auth = mockStatic(StpUtil.class)) {
            login.when(LoginHelper::isLogin).thenReturn(true);
            auth.when(() -> StpUtil.hasRoleOr(roles)).thenReturn(matches);
            auth.when(() -> StpUtil.hasPermissionOr(permissions)).thenReturn(matches);
            assertEquals(masked, new SysSensitiveServiceImpl().isSensitive(roles, permissions));
        }
    }
}
