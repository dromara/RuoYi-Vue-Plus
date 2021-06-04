package com.ruoyi.common.core.redis;


import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

	@Autowired
	private RedissonClient redissonClient;

	private static final String LOCK_TITLE = "redisLock_";

	private static final Logger log = LoggerFactory.getLogger(RedisLockUtil.class);

/*	public boolean getLock(String key){
		key = LOCK_TITLE + key;
		RLock mylock = redissonClient.getLock(key);
		System.err.println("======lock======" + Thread.currentThread().getName());
		return true;
	}*/

	/**
	 * 加锁 （RLock）带超时时间的
	 * @param key
	 * @param expire
	 * @param expireUnit
	 * @return
	 */
	public boolean acquire(String key, long expire, TimeUnit expireUnit) {
		//声明key对象
		key = LOCK_TITLE + key;
		//获取锁对象
		RLock mylock = redissonClient.getLock(key);
		//加锁，并且设置锁过期时间，防止死锁的产生
		try {
			mylock.tryLock(expire,expire,expireUnit);
		} catch (InterruptedException e) {
			 e.getMessage();
			 return false;
		}
		System.err.println("======lock======" + Thread.currentThread().getName());
		//加锁成功
		return true;
	}
	//锁的释放
	public void release(String lockName) {
		//必须是和加锁时的同一个key
		String key = LOCK_TITLE + lockName;
		//获取所对象
		RLock mylock = redissonClient.getLock(key);
		//释放锁（解锁）
		mylock.unlock();
		System.err.println("======unlock======" + Thread.currentThread().getName());
	}

}
