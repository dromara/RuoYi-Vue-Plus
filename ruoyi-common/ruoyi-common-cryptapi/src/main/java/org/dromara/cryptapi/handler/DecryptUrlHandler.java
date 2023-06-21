package org.dromara.cryptapi.handler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dromara.cryptapi.annotation.ApiDecrypt;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 获取需要解密的Url配置
 *
 * @author wdhcr
 */
@Data
@Component
@RequiredArgsConstructor
public class DecryptUrlHandler implements InitializingBean {

    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)}");

    private List<String> urls = new ArrayList<>();

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void afterPropertiesSet() {
        Set<String> set = new HashSet<>();
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        List<RequestMappingInfo> requestMappingInfos = map.entrySet().stream().filter(item -> {
            HandlerMethod method = item.getValue();
            ApiDecrypt decrypt = method.getMethodAnnotation(ApiDecrypt.class);
            // 标有解密注解的并且是post 或者put 请求的handler
            return decrypt != null && CollectionUtil.containsAny(item.getKey().getMethodsCondition().getMethods(), Arrays.asList(RequestMethod.PUT, RequestMethod.POST));
        }).map(Map.Entry::getKey).toList();
        requestMappingInfos.forEach(info -> {
            // 获取注解上边的 path 替代 path variable 为 *
            Optional.ofNullable(info.getPathPatternsCondition())
                .map(PathPatternsRequestCondition::getPatterns)
                .orElseGet(HashSet::new)
                .forEach(url -> set.add(ReUtil.replaceAll(url.getPatternString(), PATTERN, "*")));
        });
        urls.addAll(set);
    }

}
