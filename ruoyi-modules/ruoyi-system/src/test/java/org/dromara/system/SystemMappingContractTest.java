package org.dromara.system;

import io.github.linpeilie.Converter;
import org.dromara.system.domain.*;
import org.dromara.system.domain.bo.*;
import org.dromara.system.domain.vo.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SystemMappingContractTest {

    private static Stream<Arguments> records() {
        return Stream.of(
            Arguments.of(SysClientBo.class, SysClient.class, SysClientVo.class,
                Map.of("clientKey", "mobile", "deviceType", "android", "ipWhitelist", "192.0.2.1")),
            Arguments.of(SysConfigBo.class, SysConfig.class, SysConfigVo.class,
                Map.of("configKey", "sys.account.enabled", "configValue", "true")),
            Arguments.of(SysDeptBo.class, SysDept.class, SysDeptVo.class,
                Map.of("deptId", 21L, "parentId", 12L, "deptName", "仓储")),
            Arguments.of(SysDictDataBo.class, SysDictData.class, SysDictDataVo.class,
                Map.of("dictLabel", "启用", "dictValue", "0", "dictType", "sys_status")),
            Arguments.of(SysDictTypeBo.class, SysDictType.class, SysDictTypeVo.class,
                Map.of("dictName", "状态", "dictType", "sys_status")),
            Arguments.of(SysLoginInfoBo.class, SysLoginInfo.class, SysLoginInfoVo.class,
                Map.of("userName", "operator", "ipaddr", "192.0.2.1", "browser", "Firefox")),
            Arguments.of(SysMenuBo.class, SysMenu.class, SysMenuVo.class,
                Map.of("parentId", 52L, "path", "stock", "perms", "stock:list")),
            Arguments.of(SysNoticeBo.class, SysNotice.class, SysNoticeVo.class,
                Map.of("noticeTitle", "版本更新", "noticeType", "1")),
            Arguments.of(SysOperLogBo.class, SysOperLog.class, SysOperLogVo.class,
                Map.of("title", "调拨", "operName", "auditor", "requestMethod", "POST")),
            Arguments.of(SysOssBo.class, SysOss.class, SysOssVo.class,
                Map.of("originalName", "合同.pdf", "fileName", "2026/contract.pdf", "service", "archive")),
            Arguments.of(SysOssConfigBo.class, SysOssConfig.class, SysOssConfigVo.class,
                Map.of("configKey", "archive", "bucketName", "contracts")),
            Arguments.of(SysPostBo.class, SysPost.class, SysPostVo.class,
                Map.of("deptId", 7L, "postCode", "reviewer", "postCategory", "finance")),
            Arguments.of(SysRoleBo.class, SysRole.class, SysRoleVo.class,
                Map.of("roleName", "审核员", "roleKey", "reviewer", "dataScope", "5")),
            Arguments.of(SysSocialBo.class, SysSocial.class, SysSocialVo.class,
                Map.of("authId", "github-42", "source", "github", "userId", 42L)),
            Arguments.of(SysUserBo.class, SysUser.class, SysUserVo.class,
                Map.of("userId", 18L, "deptId", 3L, "userName", "reviewer", "email", "reviewer@example.org"))
        );
    }

    @ParameterizedTest(name = "{0} 业务字段创建与更新")
    @MethodSource("records")
    void businessFieldsSurviveCreateUpdateAndViewConversion(Class<?> requestType, Class<?> entityType,
            Class<?> viewType, Map<String, Object> fields) throws Exception {
        var converter = new Converter();
        Object request = requestType.getConstructor().newInstance();
        new BeanWrapperImpl(request).setPropertyValues(fields);
        Object entity = converter.convert(request, entityType);
        assertFields(entity, fields);
        Object view = converter.convert(entity, viewType);
        assertFields(view, fields);
        assertFields(converter.convert(view, entityType), fields);

        Object saved = entityType.getConstructor().newInstance();
        assertSame(saved, converter.convert(request, saved));
        assertFields(saved, fields);
        Object refreshed = viewType.getConstructor().newInstance();
        assertSame(refreshed, converter.convert(saved, refreshed));
        assertFields(refreshed, fields);
        Object restored = entityType.getConstructor().newInstance();
        assertSame(restored, converter.convert(refreshed, restored));
        assertFields(restored, fields);
    }

    private static void assertFields(Object record, Map<String, Object> fields) {
        var properties = new BeanWrapperImpl(record);
        fields.forEach((name, expected) -> assertEquals(expected, properties.getPropertyValue(name), name));
    }
}
