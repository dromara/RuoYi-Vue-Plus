package com.ruoyi.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.ruoyi.framework.jackson.BigNumberSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * jackson 配置
 *
 * @author Lion Li
 */
@Slf4j
@Configuration
public class JacksonConfig {

	@Primary
	@Bean
	public ObjectMapper getObjectMapper(Jackson2ObjectMapperBuilder builder, JacksonProperties jacksonProperties) {
		ObjectMapper objectMapper = builder.createXmlMapper(false).build();
		// 全局配置序列化返回 JSON 处理
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
		simpleModule.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
		simpleModule.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
		simpleModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(jacksonProperties.getDateFormat());
		simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
		simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
		objectMapper.registerModule(simpleModule);
		objectMapper.setTimeZone(TimeZone.getDefault());
		log.info("初始化 jackson 配置");
		return objectMapper;
	}

}
