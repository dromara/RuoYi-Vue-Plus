package org.dromara.common.web.filter;

import org.dromara.common.core.utils.StringUtils;
import org.springframework.http.MediaType;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 可重复读取请求体的过滤器，仅对 JSON 请求包装可重复消费的请求对象。
 *
 * @author ruoyi
 */
public class RepeatableFilter implements Filter {

    /**
     * 过滤器初始化入口，当前无额外初始化逻辑。
     *
     * @param filterConfig 过滤器配置
     * @throws ServletException 过滤器初始化异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    /**
     * 为 JSON 请求创建可重复读取的包装器，便于日志、验签等场景多次读取请求体。
     *
     * @param request 原始请求
     * @param response 当前响应
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        if (request instanceof HttpServletRequest
            && StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            requestWrapper = new RepeatedlyRequestWrapper((HttpServletRequest) request, response);
        }
        if (null == requestWrapper) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }
    }

    /**
     * 过滤器销毁入口，当前无额外资源需要释放。
     */
    @Override
    public void destroy() {

    }
}
