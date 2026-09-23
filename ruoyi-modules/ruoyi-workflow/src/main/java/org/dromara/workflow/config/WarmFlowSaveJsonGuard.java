package org.dromara.workflow.config;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AU-11 · def_json 容量与权限双门（D-009 · 2026-09-22 · OW决议自决授权批）
 *
 * <p>为何用拦截器而非注解：POST /warm-flow/save-json 的 controller 在
 * warm-flow-plugin jar 内（{@code WarmFlowController}），jar 代码加不了
 * {@code @SaCheckPermission}——仓内拦截器是唯一不触 jar 的落点（C 组预案 §4.2）。
 *
 * <p>两道门：
 * <ul>
 *   <li>① 容量门：Content-Length &gt; 60,000 字节 ⇒ 413（为 TEXT 65,535 留余量；
 *       把「strict 报错 / 静默截断损坏 def_json」变成显式拒绝；读 Content-Length
 *       无需缓存 body，controller 仍可正常读取）</li>
 *   <li>② 函数级门禁：{@code workflow:definition:edit}（串已有先例
 *       FlwDefinitionController.java:105；原端点任何登录用户可写 ⇒ 收紧为定义编辑者可写）</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-22
 */
public class WarmFlowSaveJsonGuard implements HandlerInterceptor {

    /** 27 号 §5.2 同源口径：TEXT 65,535 字节上限，留余量取 60,000 */
    static final long MAX_BODY_BYTES = 60_000L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // ① 容量门（AU-11 核心）
        String len = request.getHeader("Content-Length");
        if (len != null) {
            try {
                if (Long.parseLong(len) > MAX_BODY_BYTES) {
                    reject(response, 413, "def_json 超过 60000 字节上限（TEXT 65535 留余量，AU-11 容量门）");
                    return false;
                }
            } catch (NumberFormatException ignored) {
                // 非 numeric Content-Length 交给后续流程
            }
        }
        // ② 函数级门禁（jar 内 controller 加不了注解——拦截器补位）
        try {
            StpUtil.checkPermission("workflow:definition:edit");
        } catch (Exception e) {
            reject(response, 403, "无 workflow:definition:edit 权限（AU-11 函数级门）");
            return false;
        }
        return true;
    }

    private void reject(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
            ("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
