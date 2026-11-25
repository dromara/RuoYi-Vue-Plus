package org.dromara.system.controller.system;

import cn.hutool.crypto.digest.BCrypt;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfilePasswordTest {
    private final ISysUserService users = mock();
    private final SysProfileController profile = new SysProfileController(users);

    private void existingAccount() {
        var user = new SysUserVo();
        user.setUserId(42L);
        user.setPassword(BCrypt.hashpw("old-password"));
        when(users.selectUserById(42L)).thenReturn(user);
    }

    @ParameterizedTest
    @CsvSource({"wrong,new-password,修改密码失败，旧密码错误", "old-password,old-password,新密码不能与旧密码相同"})
    void rejectsInvalidPasswordChange(String oldPassword, String newPassword, String message) {
        existingAccount();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            var result = profile.updatePwd(new SysProfileController.SysUserPasswordBo(oldPassword, newPassword));
            assertTrue(R.isError(result));
            assertEquals(message, result.getMsg());
            verify(users, never()).resetUserPwd(anyLong(), anyString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void hashesNewPasswordAndReportsWriteResult(int rows) {
        existingAccount();
        when(users.resetUserPwd(eq(42L), anyString())).thenReturn(rows);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(42L);
            var result = profile.updatePwd(new SysProfileController.SysUserPasswordBo("old-password", "new-password"));
            assertEquals(rows == 1, R.isSuccess(result));
            var hash = ArgumentCaptor.forClass(String.class);
            verify(users).resetUserPwd(eq(42L), hash.capture());
            assertTrue(BCrypt.checkpw("new-password", hash.getValue()));
        }
    }

    @ParameterizedTest
    @CsvSource({"oldPassword,newPassword", "newPassword,oldPassword"})
    void missingPasswordFieldFailsRequestValidation(String provided, String missing) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(profile).build();
        var result = mvc.perform(put("/system/user/profile/updatePwd")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + provided + "\":\"password\"}"))
            .andExpect(status().isBadRequest()).andReturn();
        var error = assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException());
        assertTrue(error.getBindingResult().hasFieldErrors(missing));
        assertFalse(error.getBindingResult().hasFieldErrors(provided));
        verifyNoInteractions(users);
    }
}
