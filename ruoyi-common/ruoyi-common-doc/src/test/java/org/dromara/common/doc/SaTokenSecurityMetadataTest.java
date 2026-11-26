package org.dromara.common.doc;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.models.Operation;
import org.dromara.common.doc.core.resolver.SaTokenAnnotationMetadataJavadocResolver;
import org.springframework.web.method.HandlerMethod;
import org.dromara.common.doc.core.model.SaTokenSecurityMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("common-doc 功能单元测试")
class SaTokenSecurityMetadataTest {

    @SaCheckRole("reviewer")
    @SaCheckPermission("orders:list")
    static class OrderEndpoint {
        @SaCheckPermission(value = {"orders:create", "orders:update"}, mode = SaMode.OR, orRole = "owner")
        @SaCheckRole("editor")
        public void edit() {}

        @SaIgnore
        public void status() {}
    }

    @Test
    void documentsBothControllerAndMethodRequirements() throws Exception {
        var resolver = new SaTokenAnnotationMetadataJavadocResolver();
        var endpoint = new OrderEndpoint();
        var edit = new HandlerMethod(endpoint, OrderEndpoint.class.getMethod("edit"));
        assertTrue(resolver.supports(edit));
        String markdown = resolver.resolve(edit, new Operation());
        assertTrue(markdown.contains("`orders:list`"));
        assertTrue(markdown.contains("`orders:create` | `orders:update`"));
        assertTrue(markdown.contains("`owner`"));
        assertTrue(markdown.contains("`reviewer`"));
        assertTrue(markdown.contains("`editor`"));

        var status = new HandlerMethod(endpoint, OrderEndpoint.class.getMethod("status"));
        assertTrue(resolver.supports(status));
        String ignored = resolver.resolve(status, new Operation());
        assertTrue(ignored.contains("忽略权限检查"));
        org.junit.jupiter.api.Assertions.assertFalse(ignored.contains("orders:list"));
    }
    @Test
    @DisplayName("未识别的权限组合模式按默认 AND 展示")
    void shouldUseDefaultSymbolForUnknownMode() {
        var info = new SaTokenSecurityMetadata.AuthInfo();
        info.setMode("unknown");
        assertEquals(" & ", info.getModeSymbol());
        info.setMode("or");
        assertEquals(" | ", info.getModeSymbol());
    }

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
