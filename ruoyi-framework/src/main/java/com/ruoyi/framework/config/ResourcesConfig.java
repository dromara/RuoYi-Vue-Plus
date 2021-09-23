package com.ruoyi.framework.config;

import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {

    // 注册sa-token的拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> urlPath = Arrays.asList(
                "/login",
                "/logout",
                "/captchaImage",
                "/*.html",
                "/**/*.html",
                "/**/*.css",
                "/**/*.js",
                "/profile/**",
                "/common/download**",
                "/common/download/resource**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/webjars/**",
                "/*/api-docs",
                "/druid/**",
                "/actuator",
                "/actuator/**"
        );
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaRouteInterceptor((request, response, handler) -> {
            // 登录验证 -- 排除多个路径
            SaRouter.match(
                    //获取所有的
                    Collections.singletonList("/**"),
                    //排除下不需要拦截的
                    urlPath,
                    () -> {
                        Long userId = SecurityUtils.getUserId();
                        if(StringUtils.isNotNull(userId) ) {
                            long tokenTimeout = StpUtil.getTokenTimeout();
                            long tokenActivityTimeout = StpUtil.getTokenActivityTimeout();
                            System.out.println("剩余有效时间: " + tokenTimeout);
                            System.out.println("临时有效时间: " + tokenActivityTimeout);
                        }
                    });
        })).addPathPatterns("/**");
        registry.addInterceptor(new SaAnnotationInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 设置访问源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 对接口配置跨域设置
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
