package org.dromara.common.doc.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 存储权限框架注解解析后的权限和角色信息
 *
 * @author AprilWind
 */
@Data
@JsonInclude(Include.NON_EMPTY)
public class SaTokenSecurityMetadata {

    /**
     * 权限校验信息列表（对应 @SaCheckPermission 注解）
     */
    private List<AuthInfo> permissions = new ArrayList<>();

    /**
     * 角色校验信息列表（对应 @SaCheckRole 注解）
     */
    private List<AuthInfo> roles = new ArrayList<>();

    /**
     * 是否忽略校验（对应 @SaIgnore 注解）
     */
    private boolean ignore = false;

    /**
     * 添加权限信息
     *
     * @param values  权限值数组
     * @param mode    校验模式（AND/OR）
     * @param type    权限类型
     * @param orRoles 或角色数组
     */
    public void addPermission(String[] values, String mode, String type, String[] orRoles) {
        if (values != null && values.length > 0) {
            AuthInfo authInfo = new AuthInfo();
            authInfo.setValues(values);
            authInfo.setMode(mode);
            authInfo.setType(type);
            if (orRoles != null && orRoles.length > 0) {
                authInfo.setOrValues(orRoles);
                authInfo.setOrType("role");
            }
            this.permissions.add(authInfo);
        }
    }

    /**
     * 添加角色信息
     *
     * @param values 角色值数组
     * @param mode   校验模式（AND/OR）
     * @param type   角色类型
     */
    public void addRole(String[] values, String mode, String type) {
        if (values != null && values.length > 0) {
            AuthInfo authInfo = new AuthInfo();
            authInfo.setValues(values);
            authInfo.setMode(mode);
            authInfo.setType(type);
            this.roles.add(authInfo);
        }
    }

    /**
     * 生成 Markdown 结构的权限说明
     *
     * @return Markdown 文本
     */
    public String toMarkdownString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<br><h3>访问权限</h3><br>");

        if (ignore) {
            sb.append("> **权限策略**：忽略权限检查<br>");
            return sb.toString();
        }

        if (!ignore && permissions.isEmpty() && roles.isEmpty()){
            sb.append("> **权限策略**：需要登录<br><br>");
            return sb.toString();
        }

        if (!permissions.isEmpty()) {
            sb.append("**权限校验：**<br><br>");

            permissions.forEach(p -> {
                String permTags = Arrays.stream(p.getValues())
                    .map(v -> "`" + v + "`")
                    .collect(Collectors.joining(p.getModeSymbol()));

                sb.append("- ").append(permTags).append("<br>");

                if (p.getOrValues() != null && p.getOrValues().length > 0) {
                    String orTags = Arrays.stream(p.getOrValues())
                        .map(v -> "`" + v + "`")
                        .collect(Collectors.joining(p.getModeSymbol()));
                    sb.append("  - 或角色：").append(orTags).append("<br>");
                }
            });

            sb.append("<br>");
        }

        if (!roles.isEmpty()) {
            sb.append("**角色校验：**<br><br>");

            roles.forEach(r -> {

                String roleTags = Arrays.stream(r.getValues())
                    .map(v -> "`" + v + "`")
                    .collect(Collectors.joining(r.getModeSymbol()));

                sb.append("- ").append(roleTags).append("<br>");
            });
        }

        return sb.toString().trim();
    }

    /**
     * 认证信息
     */
    @Data
    @JsonInclude(Include.NON_EMPTY)
    public static class AuthInfo {

        /**
         * 权限或角色值数组
         */
        private String[] values;

        /**
         * 校验模式（AND/OR）
         */
        private String mode;

        /**
         * 类型说明
         */
        private String type;

        /**
         * 或权限/角色值数组（用于权限校验时的或角色校验）
         */
        private String[] orValues;

        /**
         * 或值的类型（role/permission）
         */
        private String orType;

        /**
         * 重写mode的获取方法，返回符号而非文字
         * @return AND→&，OR→|，默认→&
         */
        public String getModeSymbol() {
            if (mode == null) {
                return " & "; // 默认AND，返回&
            }
            return "AND".equalsIgnoreCase(mode) ? " & " : " | ";
        }

    }
}
