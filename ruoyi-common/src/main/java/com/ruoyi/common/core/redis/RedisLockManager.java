package com.ruoyi.common.core.redis;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * redis 锁管理类
 *
 * @author shenxinquan
 */
@Component
public class RedisLockManager {

	@Autowired
	private RedissonClient redissonClient;

	/**
	 * 通用锁
	 */
	private final static Integer BASE_LOCK = 1;

	/**
	 * 公平锁
	 */
	private final static Integer FAIR_LOCK = 2;

	/**
	 * 存放当前线程获取锁的类型
	 */
	private final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

	/**
	 * 获取锁
	 */
	private RLock getLock(String key, Integer lockType) {
		Assert.isTrue(StrUtil.isNotBlank(key), "key不能为空");
		threadLocal.set(lockType);
		RLock lock;
		if (BASE_LOCK.equals(lockType)) {
			lock = redissonClient.getLock(key);
		} else if (FAIR_LOCK.equals(lockType)) {
			lock = redissonClient.getFairLock(key);
		} else {
			throw new RuntimeException("锁不存在!");
		}
		return lock;
	}

	/**
	 * 获取锁（不用设置超时时间，一直等待）
	 */
	public boolean getLock(String key) {
		RLock lock = getLock(key, BASE_LOCK);
		return lock.tryLock();
	}

	/**
	 * 设置过期时间
	 *
	 * @param key
	 * @param time       过期时间
	 * @param expireUnit 时间单位
	 */
	public boolean getLock(String key, long time, TimeUnit expireUnit) {
		Assert.isTrue(time > 0, "过期时间必须大于0");
		Assert.isTrue(Validator.isNotEmpty(expireUnit), "时间单位不能为空");
		RLock lock = getLock(key, BASE_LOCK);
		try {
			return lock.tryLock(time, expireUnit);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 设置过期时间
	 *
	 * @param key
	 * @param waitTime   获取锁等待时间
	 * @param leaseTime  保留锁的时间
	 * @param expireUnit 时间单位
	 */
	public boolean getLock(String key, long waitTime, long leaseTime, TimeUnit expireUnit) {
		Assert.isTrue(waitTime > 0, "获取锁等待时间必须大于0");
		Assert.isTrue(leaseTime > 0, "保留锁的时间必须大于0");
		Assert.isTrue(Validator.isNotEmpty(expireUnit), "时间单位不能为空");
		RLock lock = getLock(key, BASE_LOCK);
		try {
			return lock.tryLock(waitTime, leaseTime, expireUnit);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * 获取计数器锁
	 *
	 * @param key
	 * @param count countDownLatch 的数量
	 */
	public RCountDownLatch getCountDownLatch(String key, long count) {
		Assert.isTrue(count >= 0, "count数量必须大于等于0");
		RCountDownLatch rCountDownLatch = redissonClient.getCountDownLatch(key);
		rCountDownLatch.trySetCount(count);
		return rCountDownLatch;
	}

	/**
	 * 获取公平锁
	 *
	 * @param key
	 * @param waitTime   获取锁等待时间
	 * @param leaseTime  持有锁的时间
	 * @param expireUnit 时间单位
	 * @return
	 * @throws InterruptedException
	 */
	public boolean getFairLock(String key, long waitTime, long leaseTime, TimeUnit expireUnit) {
		Assert.isTrue(waitTime > 0, "获取锁等待时间必须大于0");
		Assert.isTrue(leaseTime > 0, "保留锁的时间必须大于0");
		Assert.isTrue(Validator.isNotEmpty(expireUnit), "时间单位不能为空");
		RLock lock = getLock(key, FAIR_LOCK);
		try {
			return lock.tryLock(waitTime, leaseTime, expireUnit);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 获取公平锁
	 *
	 * @param key
	 * @param leaseTime  持有锁的时间
	 * @param expireUnit 时间单位
	 */
	public boolean getFairLock(String key, long leaseTime, TimeUnit expireUnit) {
		Assert.isTrue(leaseTime > 0, "保留锁的时间必须大于0");
		Assert.isTrue(Validator.isNotEmpty(expireUnit), "时间单位不能为空");
		RLock lock = getLock(key, FAIR_LOCK);
		try {
			return lock.tryLock(leaseTime, expireUnit);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 释放锁(统一释放)
	 */
	public void unLock(String key) {
		Integer lockType = threadLocal.get();
		RLock lock = getLock(key, lockType);
		lock.unlock();
		threadLocal.remove();
	}
}
