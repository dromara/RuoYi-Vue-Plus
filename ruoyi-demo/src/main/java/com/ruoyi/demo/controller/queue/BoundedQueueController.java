package com.ruoyi.demo.controller.queue;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.redis.QueueUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 有界队列 演示案例
 * <p>
 * 轻量级队列 重量级数据量 请使用 MQ
 * <p>
 * 集群测试通过 同一个数据只会被消费一次 做好事务补偿
 * 集群测试流程 在其中一台发送数据 两端分别调用获取接口 一次获取一条
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Slf4j
@Api(value = "有界队列 演示案例", tags = {"有界队列"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/queue/bounded")
public class BoundedQueueController {


    @ApiOperation("添加队列数据")
    @GetMapping("/add")
    public R<Void> add(@ApiParam("队列名") String queueName,
                                @ApiParam("容量") int capacity) {
        // 用完了一定要销毁 否则会一直存在
        boolean b = QueueUtils.destroyBoundedQueueObject(queueName);
        log.info("通道: {} , 删除: {}", queueName, b);
        // 初始化设置一次即可
        if (QueueUtils.trySetBoundedQueueCapacity(queueName, capacity)) {
            log.info("通道: {} , 设置容量: {}", queueName, capacity);
        } else {
            log.info("通道: {} , 设置容量失败", queueName);
            return R.fail("操作失败");
        }
        for (int i = 0; i < 11; i++) {
            String data = "data-" + i;
            boolean flag = QueueUtils.addBoundedQueueObject(queueName, data);
            if (flag == false) {
                log.info("通道: {} , 发送数据: {} 失败, 通道已满", queueName, data);
            } else {
                log.info("通道: {} , 发送数据: {}", queueName, data);
            }
        }
        return R.ok("操作成功");
    }

    @ApiOperation("删除队列数据")
    @GetMapping("/remove")
    public R<Void> remove(@ApiParam("队列名") String queueName) {
        String data = "data-" + 5;
        if (QueueUtils.removeBoundedQueueObject(queueName, data)) {
            log.info("通道: {} , 删除数据: {}", queueName, data);
        } else {
            return R.fail("操作失败");
        }
        return R.ok("操作成功");
    }

    @ApiOperation("获取队列数据")
    @GetMapping("/get")
    public R<Void> get(@ApiParam("队列名") String queueName) {
        String data;
        do {
            data = QueueUtils.getBoundedQueueObject(queueName);
            log.info("通道: {} , 获取数据: {}", queueName, data);
        } while (data != null);
        return R.ok("操作成功");
    }

}
