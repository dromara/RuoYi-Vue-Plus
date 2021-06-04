package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.RedisLock;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 测试分布式锁的样例
 *
 * @author shenxinquan
 */
@RestController
@RequestMapping("/demo/redisLock")
public class RedisLockController {

	/**
	 * #p0 标识取第一个参数为redis锁的key
	 */
	@GetMapping("/getLock")
	@RedisLock(expireTime = 10, key = "#p0")
	public AjaxResult<String> getLock(String key, String value) {
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
}
