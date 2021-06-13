package com.ruoyi.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.ruoyi.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * jackson 配置
 *
 * @author Lion Li
 */
@Slf4j
@Configuration
public class JacksonConfig {

	@Bean
	public BeanPostProcessor objectMapperBeanPostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (!(bean instanceof ObjectMapper)) {
					return bean;
				}
				ObjectMapper objectMapper = (ObjectMapper) bean;
				// 全局配置序列化返回 JSON 处理
				SimpleModule simpleModule = new SimpleModule();
				//JSON Long ==> String 把所有数字返回变为字符串返回适配前端Long型失真问题
				simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
				simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
				simpleModule.addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE);
				simpleModule.addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE);
				objectMapper.registerModule(simpleModule);
				JsonUtils.init(objectMapper);
				log.info("初始化 jackson 配置");
				return bean;
			}
		};
	}

}
