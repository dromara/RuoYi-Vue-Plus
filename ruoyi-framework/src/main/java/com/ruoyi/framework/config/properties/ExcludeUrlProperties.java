package com.ruoyi.framework.config.properties;

import cn.hutool.core.util.ReUtil;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 设置注解允许匿名访问的url
 *
 * @author Lion Li
 */
@Lazy
@Component
public class ExcludeUrlProperties implements InitializingBean {

    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    @Getter
    private final List<String> excludes = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        String asterisk = "*";
        RequestMappingHandlerMapping mapping = SpringUtils.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous -> {
                Set<PathPattern> patterns = info.getPathPatternsCondition().getPatterns();
                patterns.forEach(url -> {
                    excludes.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, asterisk));
                });
            });

            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> {
                Set<PathPattern> patterns = info.getPathPatternsCondition().getPatterns();
                patterns.forEach(url -> {
                    excludes.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, asterisk));
                });
            });
        });
    }

}
