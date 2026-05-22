package com.wudgaby.stars.config;

import com.wudgaby.stars.github.GitHubApiClient;
import org.dromara.common.encrypt.properties.EncryptorProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableAsync
@Import({StarsAiConfig.class, StarsKafkaConfig.class})
@EnableConfigurationProperties({StarsProperties.class, EncryptorProperties.class})
@MapperScan("com.wudgaby.stars.mapper")
public class StarsAutoConfiguration {

    @Bean
    public GitHubApiClient gitHubApiClient(WebClient.Builder webClientBuilder, StarsProperties properties) {
        return new GitHubApiClient(webClientBuilder, properties);
    }
}
