package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 测试分布式限流样例
 *
 * @author Lion Li
 */
@Api(value = "测试分布式限流样例", tags = {"测试分布式限流样例"})
@Slf4j
@RestController
@RequestMapping("/demo/rateLimiter")
public class RedisRateLimiterController {

	/**
	 * 测试限流注解
	 */
	@ApiOperation("测试限流注解")
	@RateLimiter(count = 2, time = 10)
	@GetMapping("/test")
	public  AjaxResult<String> test(String value){
		return AjaxResult.success("操作成功",value);
	}

}
