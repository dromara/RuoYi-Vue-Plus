package com.ruoyi.common.core.redis;


import com.ruoyi.common.annotation.RedisLock;
import com.ruoyi.common.utils.file.ImageUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁（注解实现版本）
 */
@Component
@Aspect
@Order(9)
public class RedisLockAspect {
	@Autowired
	private RedisLockUtil redisUtil;

	private static final Logger log = LoggerFactory.getLogger(RedisLockAspect.class);

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
		Object res = new Object();
		if (redisUtil.acquire(key, expireTime, TimeUnit.SECONDS)) {
			try {
				res = joinPoint.proceed();
				return res;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				redisUtil.release(key);
			}
		}else {
			throw new RuntimeException("redis分布式锁注解参数异常");
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
	 *
	 * @param key
	 * @return
	 */
	private static List<String> getKeyParsList(String key) {
		List<String> listPar = new ArrayList<>();
		if (key.contains("#")) {
			int plusIndex = key.substring(key.indexOf("#")).indexOf("+");
			int indexNext = 0;
			String parName;
			int indexPre = key.indexOf("#");
			if (plusIndex > 0) {
				indexNext = key.indexOf("#") + key.substring(key.indexOf("#")).indexOf("+");
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

}
