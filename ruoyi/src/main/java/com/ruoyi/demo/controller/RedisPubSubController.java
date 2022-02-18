package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.redis.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 发布订阅 演示案例
 *
 * @author Lion Li
 */
@Api(value = "Redis发布订阅 演示案例", tags = {"Redis发布订阅"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/redis/pubsub")
public class RedisPubSubController {

    @ApiOperation("发布消息")
    @GetMapping("/pub")
    public R<Void> pub(@ApiParam("通道Key") String key, @ApiParam("发送内容") String value) {
        RedisUtils.publish(key, value, consumer -> {
            System.out.println("发布通道 => " + key + ", 发送值 => " + value);
        });
        return R.ok("操作成功");
    }

    @ApiOperation("订阅消息")
    @GetMapping("/sub")
    public R<Void> sub(@ApiParam("通道Key") String key) {
        RedisUtils.subscribe(key, String.class, msg -> {
            System.out.println("订阅通道 => " + key + ", 接收值 => " + msg);
        });
        return R.ok("操作成功");
    }

}
