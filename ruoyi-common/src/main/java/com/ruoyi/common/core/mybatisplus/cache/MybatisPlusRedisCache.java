package com.ruoyi.common.core.mybatisplus.cache;

import cn.hutool.extra.spring.SpringUtil;
import com.ruoyi.common.utils.RedisUtils;
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
 * @deprecated 3.4.0删除 推荐使用spirng-cache
 * @author Lion Li
 */
@Slf4j
public class MybatisPlusRedisCache implements Cache {

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

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
		if (value != null) {
			RedisUtils.setCacheObject(key.toString(), value);
		}
	}

	@Override
	public Object getObject(Object key) {
		try {
			if (key != null) {
				return RedisUtils.getCacheObject(key.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("缓存出错");
		}
		return null;
	}

	@Override
	public Object removeObject(Object key) {
		if (key != null) {
			RedisUtils.deleteObject(key.toString());
		}
		return null;
	}

	@Override
	public void clear() {
		log.debug("清空缓存");
		Collection<String> keys = RedisUtils.keys("*:" + this.id + "*");
		if (!CollectionUtils.isEmpty(keys)) {
			RedisUtils.deleteObject(keys);
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
