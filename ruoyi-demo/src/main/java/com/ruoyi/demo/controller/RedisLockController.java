package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisLockManager;
import com.ruoyi.demo.service.ITestDemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
	private ITestDemoService testDemoService;

	/**
	 * 测试lock4j
	 * @param key
	 * @param value
	 * @return
	 */
	@GetMapping("/testLock4j")
	public  AjaxResult<String> testLock4j(String key,String value){
		testDemoService.testLock4j(key);
		return AjaxResult.success("操作成功",value);
	}
	@GetMapping("/testLock4jLockTemaplate")
	public  AjaxResult<String> testLock4jLockTemaplate(String key,String value){
		testDemoService.testLock4jLockTemaplate(key);
		return AjaxResult.success("操作成功",value);
	}




	/**
	 * 测试spring-cache注解
	 */
	@Cacheable(value = "test", key = "#key")
	@GetMapping("/testCache")
	public AjaxResult<String> testCache(String key) {
		return AjaxResult.success("操作成功", key);
	}
}
