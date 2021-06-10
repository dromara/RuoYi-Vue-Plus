package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.RedisLock;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisLockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;


/**
 * 测试分布式锁的样例
 *
 * @author shenxinquan
 */
@Slf4j
@RestController
@RequestMapping("/demo/redisLock")
public class RedisLockController {

	@Autowired
	private RedisLockManager redisLockManager;

	/**
	 * #p0 标识取第一个参数为redis锁的key
	 */
	@GetMapping("/testLock1")
	@RedisLock(expireTime = 10, key = "#p0")
	public AjaxResult<String> testLock1(String key, String value) {
		try {
			// 同时请求排队
//			Thread.sleep(5000);
			// 锁超时测试
			Thread.sleep(11000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return AjaxResult.success("操作成功",value);
	}

	/**
	 * 测试锁工具类
	 */
	@GetMapping("/testLock2")
	public AjaxResult<Void> testLock(String key, Long time) {
		try {
			boolean flag = redisLockManager.getLock(key, time, TimeUnit.SECONDS);
			if (flag) {
				log.info("获取锁成功: " + key);
				Thread.sleep(3000);
				redisLockManager.unLock(key);
				log.info("释放锁成功: " + key);
			} else {
				log.error("获取锁失败: " + key);
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}
		return AjaxResult.success();
	}
}
