package org.dromara.common.json.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.json.handler.BigNumberSerializer;
import org.dromara.common.json.handler.CustomDateDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

/**
 * jackson 配置
 *
 * @author Lion Li
 */
@Slf4j
@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class JacksonConfig {

    @Bean
    public SimpleModule registerJavaTimeModule() {
        // 全局配置序列化返回 JSON 处理
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
        module.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
        module.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        module.addDeserializer(Date.class, new CustomDateDeserializer());
        return module;
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonInitCustomizer() {
        return builder -> {
            builder.defaultTimeZone(TimeZone.getDefault());
            log.info("初始化 jackson 配置");
        };
    }

}
