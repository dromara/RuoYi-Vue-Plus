package com.ruoyi.framework.config;

import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForStyle;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import com.ruoyi.framework.config.properties.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * sa-token 配置
 *
 * @author Lion Li
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityProperties securityProperties;

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaRouteInterceptor((request, response, handler) -> {
            // 登录验证 -- 排除多个路径
            SaRouter
                // 获取所有的
                .match("/**")
                // 排除下不需要拦截的
                .notMatch(securityProperties.getExcludes())
                .check(() -> {
                    // 做一些访问检查
//                    if (log.isDebugEnabled()) {
//                        Long userId = LoginUtils.getUserId();
//                        if (StringUtils.isNotNull(userId)) {
//                             log.debug("剩余有效时间: {}", StpUtil.getTokenTimeout());
//                             log.debug("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
//                        }
//                    }
                });
        })).addPathPatterns("/**");
        registry.addInterceptor(new SaAnnotationInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        // Sa-Token 整合 jwt (Style模式)
        return new StpLogicJwtForStyle();
    }

}
