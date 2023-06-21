package org.dromara.cryptapi.config;

import cn.hutool.core.collection.CollectionUtil;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.dromara.cryptapi.filter.CryptoFilter;
import org.dromara.cryptapi.handler.DecryptUrlHandler;
import org.dromara.cryptapi.properties.ApiDecryptProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.List;

@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(ApiDecryptProperties.class)
public class ApiDecryptConfig {

    private final DecryptUrlHandler decryptUrlHandler;

    private final ApiDecryptProperties apiDecryptProperties;

    @Bean
    public FilterRegistrationBean<CryptoFilter> cryptoFilterRegistration() {
        FilterRegistrationBean<CryptoFilter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new CryptoFilter());
        List<String> urls = decryptUrlHandler.getUrls();
        if (CollectionUtil.isNotEmpty(urls) || apiDecryptProperties.getEnable()) {
            registration.setEnabled(true);
            registration.addUrlPatterns(urls.toArray(new String[0]));
        } else {
            registration.setEnabled(false);
        }
        registration.setName("cryptoFilter");
        HashMap<String, String> param = new HashMap<>();
        param.put(CryptoFilter.CRYPTO_PUBLIC_KEY, apiDecryptProperties.getPublicKey());
        param.put(CryptoFilter.CRYPTO_PRIVATE_KEY, apiDecryptProperties.getPrivateKey());
        param.put(CryptoFilter.CRYPTO_HEADER_FLAG, apiDecryptProperties.getHeaderFlag());
        registration.setInitParameters(param);
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        return registration;
    }
}
