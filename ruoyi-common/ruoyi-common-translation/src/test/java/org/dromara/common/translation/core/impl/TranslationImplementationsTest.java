package org.dromara.common.translation.core.impl;

import org.dromara.common.core.service.DictService;
import org.dromara.system.api.DeptService;
import org.dromara.system.api.OssService;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.OssDTO;
import org.dromara.system.api.domain.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("内置翻译实现单元测试")
class TranslationImplementationsTest {

    /**
     * 验证部门翻译兼容 Long、逗号 ID 字符串和批量映射，并忽略无法识别的键类型。
     */
    @Test
    @DisplayName("翻译部门名称")
    void shouldTranslateDepartmentNames() {
        DeptService service = mock(DeptService.class);
        when(service.selectDeptNameByIds("1")).thenReturn("研发部");
        when(service.selectDeptNameByIds("1,2")).thenReturn("研发部,财务部");
        when(service.selectDeptNamesByIds(Set.of(1L, 2L))).thenReturn(Map.of(1L, "研发部", 2L, "财务部"));
        DeptNameTranslationImpl translation = new DeptNameTranslationImpl(service);

        assertEquals("研发部", translation.translation(1L, null));
        assertEquals("研发部,财务部", translation.translation("1,2", null));
        assertNull(translation.translation(1, null));
        assertEquals(Map.of(1L, "研发部", "2,1", "财务部,研发部"),
            translation.translationBatch(new LinkedHashSet<>(List.of(1L, "2,1")), null));
    }

    /**
     * 验证昵称翻译批量查询一次后，按原始复合 ID 顺序重新组装显示值。
     */
    @Test
    @DisplayName("批量翻译用户昵称")
    void shouldTranslateNicknamesInBatch() {
        UserService service = mock(UserService.class);
        when(service.selectNicknameById(1L)).thenReturn("管理员");
        when(service.selectNicknameByIds("2,1")).thenReturn("访客,管理员");
        when(service.selectUserNicksByIds(Set.of(1L, 2L))).thenReturn(Map.of(1L, "管理员", 2L, "访客"));
        NicknameTranslationImpl translation = new NicknameTranslationImpl(service);

        assertEquals("管理员", translation.translation(1L, null));
        assertEquals("访客,管理员", translation.translation("2,1", null));
        assertNull(translation.translation(1, null));
        assertEquals(Map.of("2,1", "访客,管理员", 1L, "管理员"),
            translation.translationBatch(new LinkedHashSet<>(List.of("2,1", 1L)), null));
    }

    /**
     * 验证用户名批量翻译从 DTO 列表构建映射，并跳过没有查询结果的 ID。
     */
    @Test
    @DisplayName("批量翻译用户名")
    void shouldTranslateUserNamesFromDtoList() {
        UserService service = mock(UserService.class);
        when(service.selectUserNameById(1L)).thenReturn("admin");
        when(service.selectListByIds(Set.of(1L, 2L, 3L))).thenReturn(List.of(
            user(1L, "admin"), user(2L, "guest")));
        UserNameTranslationImpl translation = new UserNameTranslationImpl(service);

        assertEquals("admin", translation.translation("1", null));
        assertEquals(Map.of("1,3,2", "admin,guest", 2L, "guest"),
            translation.translationBatch(new LinkedHashSet<>(List.of("1,3,2", 2L)), null));
        assertEquals(Map.of(), translation.translationBatch(Set.of(), null));
        verify(service).selectListByIds(Set.of(1L, 2L, 3L));
    }

    /**
     * 验证 OSS 翻译合并所有 ID 后只查询一次，并按每个原始键恢复 URL 顺序。
     */
    @Test
    @DisplayName("批量翻译 OSS 地址")
    void shouldTranslateOssUrlsInBatch() {
        OssService service = mock(OssService.class);
        when(service.selectUrlByIds("1")).thenReturn("https://file/1");
        when(service.selectByIds("2,1")).thenReturn(List.of(
            oss(1L, "https://file/1"), oss(2L, "https://file/2")));
        OssUrlTranslationImpl translation = new OssUrlTranslationImpl(service);

        assertEquals("https://file/1", translation.translation(1L, null));
        assertNull(translation.translation(1, null));
        assertEquals(Map.of("2,1", "https://file/2,https://file/1", 1L, "https://file/1"),
            translation.translationBatch(new LinkedHashSet<>(List.of("2,1", 1L)), null));
        verify(service).selectByIds("2,1");
    }

    /**
     * 验证字典翻译处理逗号分隔值、空片段和空字典类型，并保持原键映射关系。
     */
    @Test
    @DisplayName("批量翻译字典标签")
    void shouldTranslateDictionaryLabelsInBatch() {
        DictService service = mock(DictService.class);
        when(service.getDictLabel("status", "1")).thenReturn("启用");
        when(service.getAllDictByDictType("status")).thenReturn(Map.of("0", "停用", "1", "启用"));
        DictTypeTranslationImpl translation = new DictTypeTranslationImpl(service);

        assertEquals("启用", translation.translation("1", "status"));
        assertNull(translation.translation(1L, "status"));
        assertNull(translation.translation("1", " "));
        assertEquals(Map.of("1, ,0", "启用,停用", "0", "停用"),
            translation.translationBatch(new LinkedHashSet<>(List.of("1, ,0", "0")), "status"));
        assertEquals(Map.of(), translation.translationBatch(Set.of("1"), " "));
    }

    /**
     * 创建包含用户名的测试用户 DTO。
     *
     * @param id       用户 ID
     * @param userName 用户名
     * @return 用户 DTO
     */
    private static UserDTO user(Long id, String userName) {
        UserDTO user = new UserDTO();
        user.setUserId(id);
        user.setUserName(userName);
        return user;
    }

    /**
     * 创建包含访问地址的测试 OSS DTO。
     *
     * @param id  OSS ID
     * @param url 访问地址
     * @return OSS DTO
     */
    private static OssDTO oss(Long id, String url) {
        OssDTO oss = new OssDTO();
        oss.setOssId(id);
        oss.setUrl(url);
        return oss;
    }
}
