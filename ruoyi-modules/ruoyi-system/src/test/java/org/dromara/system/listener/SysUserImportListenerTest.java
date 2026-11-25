package org.dromara.system.listener;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysUserImportVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysUserImportListenerTest {

    @ParameterizedTest
    @CsvSource({"1,true","0,false"})
    void importUpdateReportsActualWriteResult(int updated, boolean success) {
        var users = mock(ISysUserService.class);
        var config = mock(ISysConfigService.class);
        when(config.selectConfigByKey("sys.user.initPassword")).thenReturn("initial");
        var row = new SysUserImportVo();
        row.setUserName("existing");
        row.setNickName("新名称");
        var account = new SysUserVo();
        account.setUserId(51L);
        when(users.selectUserByUserName("existing")).thenReturn(account);
        when(users.updateUser(any())).thenReturn(updated);
        try (var beans = mockStatic(SpringUtil.class); var login = mockStatic(LoginHelper.class);
             var validators = Validation.buildDefaultValidatorFactory()) {
            beans.when(() -> SpringUtil.getBean(ISysConfigService.class)).thenReturn(config);
            beans.when(() -> SpringUtil.getBean(ISysUserService.class)).thenReturn(users);
            beans.when(() -> SpringUtil.getBean(Validator.class)).thenReturn(validators.getValidator());
            login.when(LoginHelper::getUserId).thenReturn(10L);
            var listener = new SysUserImportListener(true);
            listener.invoke(row, null);
            var result = listener.getExcelResult();
            verify(users).checkUserAllowed(51L);
            verify(users).checkUserDataScope(51L);
            verify(users).updateUser(argThat(user -> Long.valueOf(51).equals(user.getUserId())
                && Long.valueOf(10).equals(user.getUpdateBy()) && "新名称".equals(user.getNickName())));
            if (success) {
                assertTrue(result.getAnalysis().contains("existing 更新成功"));
            } else {
                assertTrue(assertThrows(ServiceException.class, result::getAnalysis).getMessage().contains("existing 导入失败"));
            }
        }
    }

    @Test
    void shouldCollectLookupFailureAndContinueWithNextAccount() {
        var config = mock(ISysConfigService.class);
        var users = mock(ISysUserService.class);
        when(config.selectConfigByKey("sys.user.initPassword")).thenReturn("test-password");
        var first = new SysUserImportVo();
        first.setUserName("ambiguous");
        when(users.selectUserByUserName("ambiguous")).thenThrow(new ServiceException("匹配到多个用户"));
        var next = new SysUserImportVo();
        next.setUserName("existing");
        var account = new SysUserVo();
        account.setUserName("existing");
        when(users.selectUserByUserName("existing")).thenReturn(account);

        try (var spring = mockStatic(SpringUtil.class); var login = mockStatic(LoginHelper.class)) {
            spring.when(() -> SpringUtil.getBean(ISysConfigService.class)).thenReturn(config);
            spring.when(() -> SpringUtil.getBean(ISysUserService.class)).thenReturn(users);
            var listener = new SysUserImportListener(false);
            listener.invoke(first, null);
            listener.invoke(next, null);
            var result = listener.getExcelResult();
            String report = assertThrows(ServiceException.class, result::getAnalysis).getMessage();
            assertTrue(report.contains("共 2 条"));
            assertTrue(report.contains("1、账号 ambiguous 导入失败：匹配到多个用户"));
            assertTrue(report.contains("2、账号 existing 已存在"));
            verify(users, never()).insertUser(any());
            verify(users, never()).updateUser(any());
        }
    }

    @ParameterizedTest(name = "用户导入回执，账号已存在={0}，新增条数={1}")
    @CsvSource({"false,1", "true,0", "false,0"})
    void shouldReportEachImportedAccountOnce(boolean existing, int insertedRows) {
        var config = mock(ISysConfigService.class);
        var users = mock(ISysUserService.class);
        when(config.selectConfigByKey("sys.user.initPassword")).thenReturn("test-password");
        var row = new SysUserImportVo();
        row.setUserName("report-user");
        row.setNickName("导入测试");
        if (existing) {
            var account = new SysUserVo();
            account.setUserName(row.getUserName());
            when(users.selectUserByUserName(row.getUserName())).thenReturn(account);
        } else {
            when(users.insertUser(any())).thenReturn(insertedRows);
        }

        try (var spring = mockStatic(SpringUtil.class); var login = mockStatic(LoginHelper.class)) {
            spring.when(() -> SpringUtil.getBean(ISysConfigService.class)).thenReturn(config);
            spring.when(() -> SpringUtil.getBean(ISysUserService.class)).thenReturn(users);
            spring.when(() -> SpringUtil.getBean(Validator.class))
                .thenAnswer(invocation -> Validation.buildDefaultValidatorFactory().getValidator());
            var listener = new SysUserImportListener(false);
            listener.invoke(row, null);
            var result = listener.getExcelResult();
            if (existing || insertedRows == 0) {
                String first = assertThrows(ServiceException.class, result::getAnalysis).getMessage();
                assertTrue(first.contains("共 1 条"));
                assertTrue(first.contains(existing ? "1、账号 report-user 已存在" : "1、账号 report-user 导入失败"));
                if (!existing) {
                    verify(users).insertUser(any());
                }
                assertEquals(first, assertThrows(ServiceException.class, result::getAnalysis).getMessage());
            } else {
                String first = result.getAnalysis();
                assertTrue(first.contains("1、账号 report-user 导入成功"));
                assertEquals(first, result.getAnalysis());
            }
        }
    }
}
