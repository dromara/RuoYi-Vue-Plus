package com.ruoyi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁（注解模式，不推荐使用，最好用锁的工具类）
 *
 * @author shenxinquan
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

	/**
	 * 锁过期时间 默认30秒
	 */
	int expireTime() default 30;

	/**
	 * 锁key值
	 */
	String key() default "redisLockKey";
}
