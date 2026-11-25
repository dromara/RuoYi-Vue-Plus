package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.bo.SysUserProfileBo;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileUpdateControllerTest {
    private final ISysUserService users = mock();
    private final SysProfileController controller = new SysProfileController(users);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsDuplicateContactBeforeUpdating(boolean duplicatePhone) {
        var request = new SysUserProfileBo();
        request.setPhoneNumber("13800000001");
        request.setEmail("alice@example.org");
        when(users.checkPhoneUnique(any())).thenReturn(!duplicatePhone);
        when(users.checkEmailUnique(any())).thenReturn(false);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(32L);
            var response = controller.updateProfile(request);
            assertTrue(R.isError(response));
            assertTrue(response.getMsg().contains(duplicatePhone ? "手机号码已存在" : "邮箱账号已存在"));
            verify(users, never()).updateUserProfile(any());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void writesProfileForSignedInUserAndReportsAffectedRows(int rows) {
        var request = new SysUserProfileBo();
        request.setNickName("Alice");
        when(users.updateUserProfile(any())).thenReturn(rows);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(32L);
            assertEquals(rows > 0, R.isSuccess(controller.updateProfile(request)));
            var saved = ArgumentCaptor.forClass(SysUserBo.class);
            verify(users).updateUserProfile(saved.capture());
            assertEquals(32L, saved.getValue().getUserId());
            assertEquals("Alice", saved.getValue().getNickName());
        }
    }
}
