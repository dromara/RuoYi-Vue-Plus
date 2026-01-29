package org.dromara.common.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.redis.handler.KeyPrefixHandler;
import org.dromara.common.redis.handler.RedisExceptionHandler;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.TypedJsonJackson3Codec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * redis配置
 *
 * @author Lion Li
 */
@Slf4j
@AutoConfiguration
public class RedisConfig {

    /**
     * redisson 自定义配置 （更多相关配置项目请参阅 RedissonProperties/RedissonAutoConfigurationV4 和 DataRedisProperties）
     *
     * @see org.redisson.spring.starter.RedissonProperties
     * @see org.redisson.spring.starter.RedissonAutoConfigurationV4
     * @see org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
     *
     * @param keyPrefix redis缓存key前缀
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer(@Value("${spring.data.redis.redisson.keyPrefix}") String keyPrefix) {

        return config -> {
            SimpleModule simpleModule = new SimpleModule();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
            JsonMapper jsonMapper = JsonMapper.builder()
                .addModules(simpleModule)
                .defaultTimeZone(TimeZone.getDefault())
                .changeDefaultVisibility(visibilityChecker -> visibilityChecker.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
                // 指定序列化输入的类型，类必须是非final修饰的。序列化时将对象全类名一起保存下来
                // LaissezFaireSubTypeValidator 在 Jackson 3.X 中被禁止外部引用，此处使用 DefaultBaseTypeLimitingValidator 替代，两者在行为上可能会有所差异，但一般情况下无影响
                .activateDefaultTyping(new DefaultBaseTypeLimitingValidator(), DefaultTyping.NON_FINAL)
                .build();
            // org.apache.fory.logging.LoggerFactory 包别引入错了
            // LoggerFactory.useSlf4jLogging(true);
            // ForyCodec foryCodec = new ForyCodec();
            // CompositeCodec codec = new CompositeCodec(StringCodec.INSTANCE, foryCodec, foryCodec);
            TypedJsonJackson3Codec jsonCodec = new TypedJsonJackson3Codec(Object.class, jsonMapper);
            // 组合序列化 key 使用 String 内容使用通用 json 格式
            CompositeCodec codec = new CompositeCodec(StringCodec.INSTANCE, jsonCodec, jsonCodec);
            config.setCodec(codec);
            if (SpringUtils.isVirtual()) {
                config.setNettyExecutor(new VirtualThreadTaskExecutor("redisson-"));
            }
            config.setNameMapper(new KeyPrefixHandler(keyPrefix));
            log.info("初始化 redis 配置");
        };
    }

    /**
     * 异常处理器
     */
    @Bean
    public RedisExceptionHandler redisExceptionHandler() {
        return new RedisExceptionHandler();
    }

    /**
     * redis集群配置 yml
     *
     * --- # redis 集群配置(单机与集群只能开启一个另一个需要注释掉)
     * spring.data:
     *   redis:
     *     cluster:
     *       nodes:
     *         - 192.168.0.100:6379
     *         - 192.168.0.101:6379
     *         - 192.168.0.102:6379
     *     # 密码
     *     password:
     *     # 连接超时时间
     *     timeout: 10s
     *     # 是否开启ssl
     *     ssl.enabled: false
     *     # redisson配置
     *     redisson:
     *       # 线程池数量
     *       threads: 16
     *       # Netty线程池数量
     *       nettyThreads: 32
     *       # 集群配置
     *       clusterServersConfig:
     *         # 客户端名称
     *         clientName: ${ruoyi.name}
     *         # master最小空闲连接数
     *         masterConnectionMinimumIdleSize: 32
     *         # master连接池大小
     *         masterConnectionPoolSize: 64
     *         # slave最小空闲连接数
     *         slaveConnectionMinimumIdleSize: 32
     *         # slave连接池大小
     *         slaveConnectionPoolSize: 64
     *         # 连接空闲超时，单位：毫秒
     *         idleConnectionTimeout: 10000
     *         # 命令等待超时，单位：毫秒
     *         timeout: 3000
     *         # 发布和订阅连接池大小
     *         subscriptionConnectionPoolSize: 50
     *         # 读取模式
     *         readMode: "SLAVE"
     *         # 订阅模式
     *         subscriptionMode: "MASTER"
     */

}
