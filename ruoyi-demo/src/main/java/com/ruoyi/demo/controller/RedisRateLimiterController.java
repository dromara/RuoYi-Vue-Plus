package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
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
     * 测试全局限流
     * 全局影响
     */
    @ApiOperation("测试全局限流")
    @RateLimiter(count = 2, time = 10)
    @GetMapping("/test")
    public AjaxResult<String> test(String value) {
        return AjaxResult.success("操作成功", value);
    }

    /**
     * 测试请求IP限流
     * 同一IP请求受影响
     */
    @ApiOperation("测试请求IP限流")
    @RateLimiter(count = 2, time = 10, limitType = LimitType.IP)
    @GetMapping("/testip")
    public AjaxResult<String> testip(String value) {
        return AjaxResult.success("操作成功", value);
    }

    /**
     * 测试集群实例限流
     * 启动两个后端服务互不影响
     */
    @ApiOperation("测试集群实例限流")
    @RateLimiter(count = 2, time = 10, limitType = LimitType.CLUSTER)
    @GetMapping("/testcluster")
    public AjaxResult<String> testcluster(String value) {
        return AjaxResult.success("操作成功", value);
    }

}
