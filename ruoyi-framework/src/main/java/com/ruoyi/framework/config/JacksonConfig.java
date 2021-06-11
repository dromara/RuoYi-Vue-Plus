package com.ruoyi.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 当Mybatis plus设置为雪花ID时
 * 使用此类，会把所有数字返回变为字符串返回适配前端Long型失真问题
 *
 * @author Ming LI
 */
@Configuration
public class JacksonConfig {

	@Bean
	@Primary
	@ConditionalOnMissingBean(ObjectMapper.class)
	@ConditionalOnProperty(value = "mybatis-plus.global-config.dbConfig.idType", havingValue = "ASSIGN_ID")
	public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
		ObjectMapper objectMapper = builder.createXmlMapper(false).build();
		// 全局配置序列化返回 JSON 处理
		SimpleModule simpleModule = new SimpleModule();
		//JSON Long ==> String
		simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
		objectMapper.registerModule(simpleModule);
		return objectMapper;
	}

}
