package org.dromara.common.web.filter;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.config.properties.XssProperties;
import org.springframework.http.HttpMethod;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 防止 XSS 攻击的过滤器，对非排除请求执行参数与请求体清洗。
 *
 * @author ruoyi
 */
public class XssFilter implements Filter {
    /**
     * 跳过 XSS 过滤的请求路径集合。
     */
    public List<String> excludes = new ArrayList<>();

    /**
     * 初始化过滤器并加载配置中的排除路径。
     *
     * @param filterConfig 过滤器配置
     * @throws ServletException 过滤器初始化异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        XssProperties properties = SpringUtils.getBean(XssProperties.class);
        excludes.addAll(properties.getExcludeUrls());
    }

    /**
     * 对请求执行 XSS 包装处理，命中排除规则时直接放行。
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
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (handleExcludeURL(req, resp)) {
            chain.doFilter(request, response);
            return;
        }
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(xssRequest, response);
    }

    /**
     * 判断当前请求是否需要跳过 XSS 过滤。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @return true 表示跳过过滤
     */
    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getServletPath();
        String method = request.getMethod();
        // GET DELETE 不过滤
        if (method == null || HttpMethod.GET.matches(method) || HttpMethod.DELETE.matches(method)) {
            return true;
        }
        return StringUtils.matches(url, excludes);
    }

    /**
     * 过滤器销毁入口，当前无额外资源需要释放。
     */
    @Override
    public void destroy() {

    }
}
