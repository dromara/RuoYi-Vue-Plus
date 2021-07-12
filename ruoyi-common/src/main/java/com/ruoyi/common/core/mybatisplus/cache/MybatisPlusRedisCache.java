package com.ruoyi.common.core.mybatisplus.cache;

import cn.hutool.extra.spring.SpringUtil;
import com.ruoyi.common.core.redis.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.Cache;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * mybatis-redis 二级缓存
 *
 * 使用方法 配置文件开启 mybatis-plus 二级缓存
 * 在 XxxMapper.java 类上添加注解 @CacheNamespace(implementation = MybatisPlusRedisCache.class, eviction = MybatisPlusRedisCache.class)
 *
 * @author Lion Li
 */
@Slf4j
public class MybatisPlusRedisCache implements Cache {

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private RedisCache redisCache;

	private String id;

	public MybatisPlusRedisCache(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("Cache instances require an ID");
		}
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void putObject(Object key, Object value) {
		if (redisCache == null) {
			redisCache = SpringUtil.getBean(RedisCache.class);
		}
		if (value != null) {
			redisCache.setCacheObject(key.toString(), value);
		}
	}

	@Override
	public Object getObject(Object key) {
		if (redisCache == null) {
			//由于启动期间注入失败，只能运行期间注入，这段代码可以删除
			redisCache = SpringUtil.getBean(RedisCache.class);
		}
		try {
			if (key != null) {
				return redisCache.getCacheObject(key.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("缓存出错");
		}
		return null;
	}

	@Override
	public Object removeObject(Object key) {
		if (redisCache == null) {
			redisCache = SpringUtil.getBean(RedisCache.class);
		}
		if (key != null) {
			redisCache.deleteObject(key.toString());
		}
		return null;
	}

	@Override
	public void clear() {
		log.debug("清空缓存");
		if (redisCache == null) {
			redisCache = SpringUtil.getBean(RedisCache.class);
		}
		Collection<String> keys = redisCache.keys("*:" + this.id + "*");
		if (!CollectionUtils.isEmpty(keys)) {
			redisCache.deleteObject(keys);
		}
	}

	@Override
	public int getSize() {
		RedisTemplate<String, Object> redisTemplate = SpringUtil.getBean("redisTemplate");
		Long size = redisTemplate.execute(RedisServerCommands::dbSize);
		return size.intValue();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return this.readWriteLock;
	}
}
