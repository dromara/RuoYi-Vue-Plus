package org.dromara.common.doc;

import org.dromara.common.doc.core.model.SaTokenSecurityMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("common-doc 功能单元测试")
class SaTokenSecurityMetadataTest {

    /**
     * 验证未声明权限和忽略权限时分别输出登录要求与忽略检查说明。
     */
    @Test
    @DisplayName("生成基础权限文档")
    void shouldDescribeLoginAndIgnoredSecurity() {
        SaTokenSecurityMetadata metadata = new SaTokenSecurityMetadata();
        assertTrue(metadata.toMarkdownString().contains("需要登录"));

        metadata.setIgnore(true);
        assertTrue(metadata.toMarkdownString().contains("忽略权限检查"));
    }

    /**
     * 验证权限、或角色和角色校验能够按 AND/OR 模式生成可读的 Markdown。
     */
    @Test
    @DisplayName("生成权限和角色文档")
    void shouldDescribePermissionsAndRoles() {
        SaTokenSecurityMetadata metadata = new SaTokenSecurityMetadata();
        metadata.addPermission(new String[]{"system:user:list", "system:user:query"}, "AND", "permission",
            new String[]{"admin", "auditor"});
        metadata.addRole(new String[]{"manager", "operator"}, "OR", "role");

        String markdown = metadata.toMarkdownString();

        assertTrue(markdown.contains("`system:user:list` & `system:user:query`"));
        assertTrue(markdown.contains("或角色：`admin` & `auditor`"));
        assertTrue(markdown.contains("`manager` | `operator`"));
        assertEquals(1, metadata.getPermissions().size());
        assertEquals(1, metadata.getRoles().size());
    }
}
