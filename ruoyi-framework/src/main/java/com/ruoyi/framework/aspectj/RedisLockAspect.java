package com.ruoyi.framework.aspectj;


import com.ruoyi.common.annotation.RedisLock;
import com.ruoyi.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁（注解实现版本）
 *
 * @author shenxinquan
 */

@Slf4j
@Aspect
@Order(9)
@Component
public class RedisLockAspect {

	@Autowired
	private RedissonClient redissonClient;

	@Pointcut("@annotation(com.ruoyi.common.annotation.RedisLock)")
	public void annotationPointcut() {
	}

	@Around("annotationPointcut()")
	public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		// 获得当前访问的class
		Class<?> className = joinPoint.getTarget().getClass();
		// 获得访问的方法名
		String methodName = joinPoint.getSignature().getName();
		// 得到方法的参数的类型
		Class<?>[] argClass = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();
		Object[] args = joinPoint.getArgs();
		String key = "";
		// 默认30秒过期时间
		int expireTime = 30;

		try {
			// 得到访问的方法对象
			Method method = className.getMethod(methodName, argClass);
			method.setAccessible(true);
			// 判断是否存在@RedisLock注解
			if (method.isAnnotationPresent(RedisLock.class)) {
				RedisLock annotation = method.getAnnotation(RedisLock.class);
				key = getRedisKey(args, annotation.key());
				expireTime = getExpireTime(annotation);
			}
		} catch (Exception e) {
			throw new RuntimeException("redis分布式锁注解参数异常", e);
		}

		// 声明锁名称
		key = Constants.REDIS_LOCK_KEY + key;
		Object res;
		try {
			if (acquire(key, expireTime, TimeUnit.SECONDS)) {
				try {
					res = joinPoint.proceed();
					return res;
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					release(key);
				}
			} else {
				throw new RuntimeException("redis分布式锁注解参数异常");
			}
		} catch (IllegalMonitorStateException e) {
			log.error("lock timeout => key : " + key + " , ThreadName : " + Thread.currentThread().getName());
			throw new RuntimeException("lock timeout => key : " + key);
		} catch (Exception e) {
			throw new Exception("redis分布式未知异常", e);
		}
	}

	private int getExpireTime(RedisLock annotation) {
		return annotation.expireTime();
	}

	private String getRedisKey(Object[] args, String primalKey) {
		if (args.length == 0) {
			return primalKey;
		}
		// 获取#p0...集合
		List<String> keyList = getKeyParsList(primalKey);
		for (String keyName : keyList) {
			int keyIndex = Integer.parseInt(keyName.toLowerCase().replace("#p", ""));
			Object parValue = args[keyIndex];
			primalKey = primalKey.replace(keyName, String.valueOf(parValue));
		}
		return primalKey.replace("+", "").replace("'", "");
	}

	/**
	 * 获取key中#p0中的参数名称
	 */
	private static List<String> getKeyParsList(String key) {
		List<String> listPar = new ArrayList<>();
		if (key.contains("#")) {
			int plusIndex = key.substring(key.indexOf("#")).indexOf("+");
			int indexNext = 0;
			String parName;
			int indexPre = key.indexOf("#");
			if (plusIndex > 0) {
				indexNext = key.indexOf("#") + plusIndex;
				parName = key.substring(indexPre, indexNext);
			} else {
				parName = key.substring(indexPre);
			}
			listPar.add(parName.trim());
			key = key.substring(indexNext + 1);
			if (key.contains("#")) {
				listPar.addAll(getKeyParsList(key));
			}
		}
		return listPar;
	}

	/**
	 * 加锁（RLock）带超时时间的
	 */
	private boolean acquire(String key, long expire, TimeUnit expireUnit) {
		try {
			//获取锁对象
			RLock mylock = redissonClient.getLock(key);
			//加锁，并且设置锁过期时间，防止死锁的产生
			mylock.tryLock(expire, expire, expireUnit);
		} catch (InterruptedException e) {
			return false;
		}
		log.info("lock => key : " + key + " , ThreadName : " + Thread.currentThread().getName());
		//加锁成功
		return true;
	}

	/**
	 * 锁的释放
	 */
	private void release(String lockName) {
		//获取所对象
		RLock mylock = redissonClient.getLock(lockName);
		//释放锁（解锁）
		mylock.unlock();
		log.info("unlock => key : " + lockName + " , ThreadName : " + Thread.currentThread().getName());
	}

}
