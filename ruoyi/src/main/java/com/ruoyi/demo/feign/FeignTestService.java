package com.ruoyi.demo.feign;

import com.ruoyi.demo.feign.constant.FeignTestConstant;
import com.ruoyi.demo.feign.fallback.FeignTestFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * feign测试service
 * 规范接口 Service 无感调用
 * 常量管理请求路径 更加规范
 * 自定义容错处理 安全可靠 (需自行配置熔断器)
 * 增加 feign 的目的为使 http 请求接口化
 *
 * @author Lion Li
 * @deprecated 由于使用人数较少 决定与 3.4.0 版本移除
 */
@FeignClient(
	name = FeignTestConstant.BAIDU_NAME,
	url = FeignTestConstant.BAIDU_URL,
	fallback = FeignTestFallback.class)
public interface FeignTestService {

    @GetMapping("/s")
    String search(@RequestParam("wd") String wd);
}
