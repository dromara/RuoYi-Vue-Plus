package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerLookupTest {

    private final ISysUserService users = mock();
    private final ISysRoleService roles = mock();
    private final ISysPostService posts = mock();
    private final SysUserController admin = new SysUserController(users, roles, posts, mock(ISysDeptService.class));
    private final SysProfileController profile = new SysProfileController(users);

    @Test
    void missingUserDetailsReturnFailureBeforeLoadingOptions() {
        assertTrue(R.isError(admin.getInfo(42L)));
        verifyNoInteractions(roles, posts);
    }

    @Test
    void missingRoleAssignmentTargetReturnsFailure() {
        assertTrue(R.isError(admin.authRole(42L)));
        verifyNoInteractions(roles);
    }

    @Test
    void newUserFormStillLoadsAvailableRoles() {
        assertTrue(R.isSuccess(admin.getInfo((Long) null)));
        verify(roles).selectRoleList(any());
    }

    @Test
    void missingProfileReturnsFailure() {
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            assertTrue(R.isError(profile.profile()));
            verify(users, never()).selectUserRoleGroup(anyLong());
        }
    }

    @Test
    void missingAccountCannotUpdatePassword() {
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            assertTrue(R.isError(profile.updatePwd(new SysProfileController.SysUserPasswordBo("old", "new"))));
            verify(users, never()).resetUserPwd(anyLong(), anyString());
        }
    }
}
