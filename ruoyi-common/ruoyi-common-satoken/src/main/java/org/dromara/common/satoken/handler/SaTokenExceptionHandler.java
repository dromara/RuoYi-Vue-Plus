package org.dromara.common.satoken.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * SaToken异常处理器
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 处理权限码校验失败异常。
     *
     * @param e       异常信息
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public R<Void> handleNotAccessException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String reason = e instanceof NotRoleException ? "角色权限校验失败" : "权限码校验失败";
        log.error("请求地址'{}',{}'{}'", requestURI, reason, e.getMessage());
        return R.fail(HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权");
    }

    /**
     * 处理未登录或登录态失效异常。
     *
     * @param e       异常信息
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String msg = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未检测到登录信息，请重新登录";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效，请重新登录";
            case NotLoginException.TOKEN_TIMEOUT -> "登录已过期，请重新登录";
            case NotLoginException.BE_REPLACED -> "当前账号已在其他设备登录，您已被强制下线";
            case NotLoginException.KICK_OUT -> "账号已被管理员强制下线";
            case NotLoginException.TOKEN_FREEZE -> "账号已被冻结，请联系管理员处理";
            case NotLoginException.NO_PREFIX -> "登录格式异常，请重新登录";
            case "-100" -> "客户端ID与Token不匹配";
            default -> "登录状态异常，请重新登录";
        };
        log.error("请求地址'{}',{}", requestURI, msg);
        return R.fail(HttpStatus.HTTP_UNAUTHORIZED, "认证失败，无法访问系统资源");
    }

}
