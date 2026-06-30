package org.dromara.common.redis.config;

import cn.hutool.http.HttpStatus;
import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.spring.boot.autoconfigure.LockAutoConfiguration;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Lock4j 配置
 *
 * @author AprilWind
 */
@AutoConfiguration(before = LockAutoConfiguration.class)
public class Lock4jConfig {

    /**
     * Lock4j 获取锁失败策略。
     */
    @Bean
    public LockFailureStrategy defaultLockFailureStrategy() {
        return (key, method, arguments) -> {
            throw new ServiceException("业务处理中，请稍后再试...", HttpStatus.HTTP_UNAVAILABLE);
        };
    }

}
