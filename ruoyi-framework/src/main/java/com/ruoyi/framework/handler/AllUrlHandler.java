package com.ruoyi.framework.handler;

import cn.hutool.core.util.ReUtil;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 获取所有Url配置
 *
 * @author Lion Li
 */
@Data
public class AllUrlHandler implements InitializingBean {

    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    private List<String> urls = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        RequestMappingHandlerMapping mapping = SpringUtils.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        map.keySet().forEach(info -> {
            // 获取方法上边的注解 替代path variable 为 *
            Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                    .forEach(url -> urls.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, "*")));
            // 获取类上边的注解, 替代path variable 为 *
            Objects.requireNonNull(info.getPathPatternsCondition().getPatterns())
                    .forEach(url -> urls.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, "*")));
        });
    }

}
