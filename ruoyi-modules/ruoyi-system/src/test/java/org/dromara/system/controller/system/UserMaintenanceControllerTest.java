package org.dromara.system.controller.system;

import cn.hutool.crypto.digest.BCrypt;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.service.*;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserMaintenanceControllerTest {

    @Test
    void exportUsesSubmittedUserSelection() {
        var filter = new SysUserBo();
        filter.setUserIds("3,7");
        filter.setExcludeUserIds("9");
        var rows = java.util.List.of(new org.dromara.system.domain.vo.SysUserExportVo());
        when(users.selectUserExportList(filter)).thenReturn(rows);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        try (var excel = mockStatic(org.dromara.common.excel.utils.ExcelBuilder.class, RETURNS_DEEP_STUBS)) {
            controller.export(filter, response);
            verify(users).selectUserExportList(filter);
            excel.verify(() -> org.dromara.common.excel.utils.ExcelBuilder.of(rows,
                org.dromara.system.domain.vo.SysUserExportVo.class));
        }
    }

    private final ISysUserService users = mock();
    private final ISysDeptService depts = mock();
    private final SysUserController controller = new SysUserController(users, mock(), mock(), depts);

    @ParameterizedTest
    @CsvSource({"true,name,登录账号已存在","false,name,登录账号已存在","true,phone,手机号码已存在",
        "false,phone,手机号码已存在","true,email,邮箱账号已存在","false,email,邮箱账号已存在"})
    void conflictingAccountFieldsRejectEntireWrite(boolean create, String field, String reason) {
        var user = new SysUserBo();
        user.setUserId(8L);
        user.setDeptId(21L);
        user.setUserName("alice");
        user.setEmail("alice@example.org");
        user.setPhoneNumber("13800000000");
        when(users.checkUserNameUnique(user)).thenReturn(!field.equals("name"));
        when(users.checkPhoneUnique(user)).thenReturn(!field.equals("phone"));
        when(users.checkEmailUnique(user)).thenReturn(!field.equals("email"));

        var result = create ? controller.add(user) : controller.edit(user);
        assertTrue(R.isError(result));
        assertTrue(result.getMsg().endsWith(reason));
        verify(depts).checkDeptDataScope(21L);
        verify(users, never()).insertUser(any());
        verify(users, never()).updateUser(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptsAccountWithoutOptionalContactDetails(boolean create) {
        var request = new SysUserBo();
        request.setDeptId(21L);
        request.setUserId(8L);
        request.setRoleIds(new Long[]{10L});
        request.setPostIds(new Long[]{15L});
        request.setPassword("initial-password");
        when(users.checkUserNameUnique(request)).thenReturn(true);
        when(users.insertUser(request)).thenReturn(1);
        when(users.updateUser(request)).thenReturn(1);
        var result = create ? controller.add(request) : controller.edit(request);
        assertTrue(R.isSuccess(result));
        verify(depts).checkDeptDataScope(21L);
        verify(users, never()).checkPhoneUnique(any());
        verify(users, never()).checkEmailUnique(any());
        if (create) {
            verify(users).insertUser(request);
            assertTrue(BCrypt.checkpw("initial-password", request.getPassword()));
        } else {
            verify(users).updateUser(request);
            verify(users).checkUserDataScope(8L);
        }
        assertArrayEquals(new Long[]{10L}, request.getRoleIds());
        assertArrayEquals(new Long[]{15L}, request.getPostIds());
    }

    @Test
    void refusesWholeBatchContainingSignedInAccount() {
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(8L);
            assertTrue(R.isError(controller.remove(new Long[]{7L, 8L})));
            verifyNoInteractions(users);
        }
    }
}
