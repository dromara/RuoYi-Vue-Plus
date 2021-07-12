package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * spring-cache 演示案例
 *
 * @author Lion Li
 */
// 类级别 缓存统一配置
//@CacheConfig(cacheNames = "redissonCacheMap")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/cache")
public class RedisCacheController {

	/**
	 * 测试 @Cacheable
	 *
	 * 表示这个方法有了缓存的功能,方法的返回值会被缓存下来
	 * 下一次调用该方法前,会去检查是否缓存中已经有值
	 * 如果有就直接返回,不调用方法
	 * 如果没有,就调用方法,然后把结果缓存起来
	 * 这个注解「一般用在查询方法上」
	 *
	 * cacheNames 为配置文件内 groupId
	 */
	@Cacheable(cacheNames = "redissonCacheMap", key = "#key", condition = "#key != null")
	@GetMapping("/test1")
	public AjaxResult<String> test1(String key, String value){
		return AjaxResult.success("操作成功", value);
	}

	/**
	 * 测试 @CachePut
	 *
	 * 加了@CachePut注解的方法,会把方法的返回值put到缓存里面缓存起来,供其它地方使用
	 * 它「通常用在新增方法上」
	 *
	 * cacheNames 为 配置文件内 groupId
	 */
	@CachePut(cacheNames = "redissonCacheMap", key = "#key", condition = "#key != null")
	@GetMapping("/test2")
	public AjaxResult<String> test2(String key, String value){
		return AjaxResult.success("操作成功", value);
	}

	/**
	 * 测试 @CacheEvict
	 *
	 * 使用了CacheEvict注解的方法,会清空指定缓存
	 * 「一般用在更新或者删除的方法上」
	 *
	 * cacheNames 为 配置文件内 groupId
	 */
	@CacheEvict(cacheNames = "redissonCacheMap", key = "#key", condition = "#key != null")
	@GetMapping("/test3")
	public AjaxResult<String> test3(String key, String value){
		return AjaxResult.success("操作成功", value);
	}

}
