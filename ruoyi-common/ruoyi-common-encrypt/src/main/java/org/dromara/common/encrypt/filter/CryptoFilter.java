package org.dromara.common.encrypt.filter;

import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.encrypt.properties.ApiDecryptProperties;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;


/**
 * Crypto 过滤器
 *
 * @author wdhcr
 */
public class CryptoFilter implements Filter {
    private final ApiDecryptProperties properties;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final HandlerExceptionResolver handlerExceptionResolver;

    /**
     * 构造加解密过滤器。
     *
     * @param properties                   API 解密配置
     * @param requestMappingHandlerMapping 请求映射处理器
     * @param handlerExceptionResolver     异常处理器
     */
    public CryptoFilter(ApiDecryptProperties properties,
                        RequestMappingHandlerMapping requestMappingHandlerMapping,
                        HandlerExceptionResolver handlerExceptionResolver) {
        this.properties = properties;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.handlerExceptionResolver = handlerExceptionResolver;
        EncryptUtils.validateRsaPublicKey(properties.getPublicKey());
        EncryptUtils.validateRsaPrivateKey(properties.getPrivateKey());
    }

    /**
     * 根据接口注解与请求头执行请求解密和响应加密。
     *
     * @param request  原始请求
     * @param response 原始响应
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        // 获取加密注解
        ApiEncrypt apiEncrypt = this.getApiEncryptAnnotation(servletRequest);
        boolean responseFlag = apiEncrypt != null && apiEncrypt.response();
        ServletRequest requestWrapper = null;
        ServletResponse responseWrapper = null;
        EncryptResponseBodyWrapper responseBodyWrapper = null;

        // 是否为 put 或者 post 请求
        if (HttpMethod.PUT.matches(servletRequest.getMethod()) || HttpMethod.POST.matches(servletRequest.getMethod())) {
            // 是否存在加密标头
            String headerValue = servletRequest.getHeader(properties.getHeaderFlag());
            if (StringUtils.isNotBlank(headerValue)) {
                // 请求解密
                requestWrapper = new DecryptRequestBodyWrapper(servletRequest, properties.getPrivateKey(), properties.getHeaderFlag());
            } else {
                // 是否有注解，有就报错，没有放行
                if (ObjectUtil.isNotNull(apiEncrypt)) {
                    handlerExceptionResolver.resolveException(
                        servletRequest, servletResponse, null,
                        new ServiceException("没有访问权限，请联系管理员授权", HttpStatus.FORBIDDEN));
                    return;
                }
            }
        }

        // 判断是否响应加密
        if (responseFlag) {
            responseBodyWrapper = new EncryptResponseBodyWrapper(servletResponse);
            responseWrapper = responseBodyWrapper;
        }

        chain.doFilter(
            ObjectUtil.defaultIfNull(requestWrapper, request),
            ObjectUtil.defaultIfNull(responseWrapper, response));

        if (responseFlag) {
            // 对原始内容加密
            String encryptContent = responseBodyWrapper.getEncryptContent(
                servletResponse, properties.getPublicKey(), properties.getHeaderFlag());
            // 对加密后的内容写出
            servletResponse.getWriter().write(encryptContent);
        }
    }

    /**
     * 获取 ApiEncrypt 注解
     *
     * @param servletRequest 当前请求
     * @return API 加密注解
     */
    private ApiEncrypt getApiEncryptAnnotation(HttpServletRequest servletRequest) {
        // 获取注解
        try {
            HandlerExecutionChain mappingHandler = requestMappingHandlerMapping.getHandler(servletRequest);
            if (ObjectUtil.isNotNull(mappingHandler)) {
                Object handler = mappingHandler.getHandler();
                if (ObjectUtil.isNotNull(handler)) {
                    // 从handler获取注解
                    if (handler instanceof HandlerMethod handlerMethod) {
                        return handlerMethod.getMethodAnnotation(ApiEncrypt.class);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 销毁过滤器。
     */
    @Override
    public void destroy() {
    }
}
