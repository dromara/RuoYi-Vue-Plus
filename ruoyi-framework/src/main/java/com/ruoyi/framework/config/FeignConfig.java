package com.ruoyi.framework.config;

import feign.*;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * openfeign配置类
 *
 * @author Lion Li
 */
@EnableFeignClients("${feign.package}")
@Configuration
@ConditionalOnClass(Feign.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignConfig {

    @Bean
    public OkHttpClient okHttpClient(){
        return new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();
    }

    @Bean
    public Contract feignContract() {
        return new SpringMvcContract();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(10, TimeUnit.SECONDS, 60,TimeUnit.SECONDS,true);
    }

    @Bean
    public Retryer feignRetry() {
        return new Retryer.Default();
    }

}
