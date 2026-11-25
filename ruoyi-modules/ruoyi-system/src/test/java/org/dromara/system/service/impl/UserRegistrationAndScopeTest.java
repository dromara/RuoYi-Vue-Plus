package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRegistrationAndScopeTest {

    @ParameterizedTest
    @CsvSource({"true,0","true,1","false,0","false,1"})
    void deletingUserRemovesBothRelationsAndRequiresDeletedUser(boolean single, int rows) {
        var roleLinks = mock(SysUserRoleMapper.class, RETURNS_DEEP_STUBS);
        var postLinks = mock(SysUserPostMapper.class, RETURNS_DEEP_STUBS);
        var accountService = spy(new SysUserServiceImpl(users, mock(), roles, mock(), roleLinks, postLinks));
        doNothing().when(accountService).checkUserAllowed(anyLong());
        doNothing().when(accountService).checkUserDataScope(anyLong());
        var roleQuery = roleLinks.lambda();
        var postQuery = postLinks.lambda();
        var ids = List.of(51L);
        when(roleQuery.eq(any(), eq(51L))).thenReturn(roleQuery);
        when(postQuery.eq(any(), eq(51L))).thenReturn(postQuery);
        when(roleQuery.in(any(), eq(ids))).thenReturn(roleQuery);
        when(postQuery.in(any(), eq(ids))).thenReturn(postQuery);
        when(users.deleteById(51L)).thenReturn(rows);
        when(users.deleteByIds(ids)).thenReturn(rows);
        java.util.function.IntSupplier delete = () -> single ? accountService.deleteUserById(51L)
            : accountService.deleteUserByIds(new Long[]{51L});
        if (rows == 0) {
            assertThrows(ServiceException.class, delete::getAsInt);
        } else {
            assertEquals(1, delete.getAsInt());
        }
        verify(roleQuery).delete();
        verify(postQuery).delete();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void replacesOnlySelectedUsersRoleLinks(boolean inserted) {
        var links = mock(SysUserRoleMapper.class, RETURNS_DEEP_STUBS);
        var accountService = new SysUserServiceImpl(users, mock(), roles, mock(), links, mock());
        var query = links.lambda();
        when(query.eq(any(), eq(55L))).thenReturn(query);
        when(roles.selectRoleCount(List.of(12L, 13L))).thenReturn(2L);
        when(links.insertBatch(anyList())).thenReturn(inserted);
        try (var login = mockStatic(LoginHelper.class)) {
            if (inserted) {
                accountService.insertUserAuth(55L, new Long[]{12L, 13L});
            } else {
                assertThrows(ServiceException.class, () -> accountService.insertUserAuth(55L, new Long[]{12L, 13L}));
            }
            verify(query).delete();
            verify(links).insertBatch(argThat(items -> items.size() == 2 && items.stream()
                .allMatch(link -> Long.valueOf(55L).equals(link.getUserId()))
                && items.stream().map(link -> link.getRoleId()).toList().equals(List.of(12L, 13L))));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void invisibleOrProtectedAccountAbortsBatchBeforeUnlinking(boolean protectedAccount) {
        var userId = protectedAccount ? SystemConstants.SUPER_ADMIN_USER_ID : 55L;
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(() -> LoginHelper.isSuperAdmin(userId)).thenReturn(protectedAccount);
            when(users.countUserById(32L)).thenReturn(1L);
            assertThrows(ServiceException.class, () -> service.deleteUserByIds(new Long[]{32L, userId}));
            verifyNoInteractions(assignments);
            verify(users, never()).deleteByIds(anyCollection());
        }
    }

    private final SysUserMapper users = mock();
    private final SysRoleMapper roles = mock();
    private final SysUserRoleMapper assignments = mock();
    private final SysUserServiceImpl service = new SysUserServiceImpl(users, mock(), roles, mock(), assignments, mock());

    @ParameterizedTest
    @ValueSource(ints = {0,1})
    void registrationUsesSystemAuditIdentityAndReportsAffectedRows(int rows) {
        var request = new SysUserBo();
        var record = new SysUser();
        when(users.insert(record)).thenReturn(rows);
        try (var beans = mockStatic(SpringUtil.class); var mappings = mockStatic(MapstructUtils.class)) {
            mappings.when(() -> MapstructUtils.convert(request, SysUser.class)).thenAnswer(call -> {
                assertEquals(0L, request.getCreateBy());
                assertEquals(0L, request.getUpdateBy());
                return record;
            });
            assertEquals(rows == 1, service.registerUser(request));
            verify(users).insert(record);
        }
    }

    @Test
    void emptyRoleArrayPreservesExistingAssignments() {
        service.insertUserAuth(35L, new Long[0]);
        verifyNoInteractions(assignments, roles);
    }

    @Test
    void restrictedRolesAreRejectedBeforeExistingAssignmentsAreRemoved() {
        try (var login = mockStatic(LoginHelper.class)) {
            assertThrows(ServiceException.class,
                () -> service.insertUserAuth(35L, new Long[]{SystemConstants.SUPER_ADMIN_ROLE_ID}));
            when(roles.selectRoleCount(List.of(12L))).thenReturn(0L);
            assertThrows(ServiceException.class, () -> service.insertUserAuth(35L, new Long[]{12L}));
            verifyNoInteractions(assignments);
        }
    }

    @Test
    void nicknameLookupSkipsMissingRecordsAndPreservesRequestOrder() {
        var lookup = mock(SysUserServiceImpl.class);
        when(lookup.selectNicknameById(3L)).thenReturn("乙");
        when(lookup.selectNicknameById(1L)).thenReturn("甲");
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            assertEquals("乙,甲", service.selectNicknameByIds("3,2,1"));
        }
    }

    @Test
    void emptyAssociationFiltersReturnNoUsers() {
        assertTrue(service.selectUserIdsByPostIds(List.of()).isEmpty());
        assertTrue(service.selectUserIdsByDeptIds(List.of()).isEmpty());
        assertTrue(service.selectUserIdsByRoleIds(List.of()).isEmpty());
        verifyNoInteractions(users, roles, assignments);
    }
}
