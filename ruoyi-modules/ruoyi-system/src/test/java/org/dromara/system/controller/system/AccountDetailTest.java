package org.dromara.system.controller.system;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.domain.vo.*;
import org.dromara.system.service.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountDetailTest {

    @Test
    void accountWithoutDepartmentDoesNotLookUpDepartmentPosts() {
        var user = new SysUserVo();
        when(users.selectUserById(32L)).thenReturn(user);
        var response = controller.getInfo(32L);
        assertSame(user, response.getData().getUser());
        verifyNoInteractions(posts);
    }

    @Test
    void sessionWithoutVisibleBusinessUserGetsFailure() {
        var session = new LoginUser();
        session.setUserId(23L);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(session);
            assertTrue(org.dromara.common.core.domain.R.isError(controller.getInfo()));
            verify(users).selectUserById(23L);
        }
    }

    private final ISysUserService users = mock();
    private final ISysRoleService roles = mock();
    private final ISysPostService posts = mock();
    private final SysUserController controller = new SysUserController(users, roles, posts, mock());

    @Test
    void signedInAccountUsesSessionPermissionsAndProfileGroups() {
        var session = new LoginUser();
        session.setUserId(23L);
        session.setRolePermission(Set.of("buyer"));
        session.setMenuPermission(Set.of("order:list"));
        var account = new SysUserVo();
        account.setUserId(23L);
        account.setNickName("采购员");
        when(users.selectUserById(23L)).thenReturn(account);
        when(users.selectUserRoleGroup(23L)).thenReturn("采购经理");
        when(users.selectUserPostGroup(23L)).thenReturn("采购岗");
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(session);
            login.when(LoginHelper::getUserId).thenReturn(23L);
            var info = controller.getInfo().getData();
            assertSame(account, info.getUser());
            assertEquals(Set.of("buyer"), info.getRoles());
            assertEquals(Set.of("order:list"), info.getPermissions());
            var profile = new SysProfileController(users).profile().getData();
            assertEquals("采购员", profile.user().getNickName());
            assertEquals("采购经理", profile.roleGroup());
            assertEquals("采购岗", profile.postGroup());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void assignedAccountDetailFiltersAdministratorRole(boolean admin) {
        long id = admin ? SystemConstants.SUPER_ADMIN_USER_ID : 23L;
        var account = new SysUserVo();
        account.setDeptId(9L);
        var superRole = new SysRoleVo();
        superRole.setRoleId(SystemConstants.SUPER_ADMIN_ROLE_ID);
        var ordinary = new SysRoleVo();
        ordinary.setRoleId(4L);
        var available = List.of(superRole, ordinary);
        var positions = List.of(new SysPostVo());
        when(users.selectUserById(id)).thenReturn(account);
        when(roles.selectRoleList(any())).thenReturn(available);
        when(roles.selectRolesAuthByUserId(id)).thenReturn(available);
        when(roles.selectRoleListByUserId(id)).thenReturn(List.of(4L));
        when(posts.selectPostList(argThat(post -> Long.valueOf(9L).equals(post.getDeptId())))).thenReturn(positions);
        when(posts.selectPostListByUserId(id)).thenReturn(List.of(11L));
        var info = controller.getInfo(id).getData();
        assertSame(account, info.getUser());
        assertEquals(positions, info.getPosts());
        assertEquals(List.of(11L), info.getPostIds());
        assertEquals(List.of(4L), info.getRoleIds());
        var expected = admin ? available : List.of(ordinary);
        assertEquals(expected, info.getRoles());
        assertEquals(expected, controller.authRole(id).getData().getRoles());
    }
}
