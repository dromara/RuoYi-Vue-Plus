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

//	/**
//	 * 自定义异常解码器
//	 * 用于自定义返回体异常熔断
//	 */
//	@Bean
//	public ErrorDecoder errorDecoder() {
//		return new CustomErrorDecoder();
//	}
//
//
//	/**
//	 * 自定义返回体解码器
//	 */
//	@Slf4j
//	public static class CustomErrorDecoder implements ErrorDecoder {
//
//		@Override
//		public Exception decode(String methodKey, Response response) {
//			Exception exception = null;
//			try {
//				// 获取原始的返回内容
//				String json = JsonUtils.toJsonString(response.body().asReader(StandardCharsets.UTF_8));
//				exception = new RuntimeException(json);
//				// 将返回内容反序列化为Result，这里应根据自身项目作修改
//				AjaxResult result = JsonUtils.parseObject(json, AjaxResult.class);
//				// 业务异常抛出简单的 RuntimeException，保留原来错误信息
//				if (result.getCode() != 200) {
//					exception = new RuntimeException(result.getMsg());
//				}
//			} catch (IOException e) {
//				log.error(e.getMessage(), e);
//			}
//			return exception;
//		}
//	}

}
