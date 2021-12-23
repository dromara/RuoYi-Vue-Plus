package com.ruoyi.demo.controller.queue;

import cn.hutool.core.util.RandomUtil;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.redis.QueueUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优先队列 演示案例
 * <p>
 * 轻量级队列 重量级数据量 请使用 MQ
 * <p>
 * 集群测试通过 同一个消息只会被消费一次 做好事务补偿
 * 集群测试流程 在其中一台发送数据 两端分别调用获取接口 一次获取一条
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Slf4j
@Api(value = "优先队列 演示案例", tags = {"优先队列"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/queue/priority")
public class PriorityQueueController {

    @ApiOperation("添加队列数据")
    @GetMapping("/add")
    public AjaxResult<Void> add(@ApiParam("队列名") String queueName) {
        // 用完了一定要销毁 否则会一直存在
        boolean b = QueueUtils.destroyPriorityQueueObject(queueName);
        log.info("通道: {} , 删除: {}", queueName, b);
        // 初始化设置一次即可 此处注意 不允许用内部类或匿名类
        boolean flag = QueueUtils.trySetPriorityQueueComparator(queueName, new PriorityDemoComparator());
        if (flag) {
            log.info("通道: {} , 设置比较器成功", queueName);
        } else {
            log.info("通道: {} , 设置比较器失败", queueName);
            return AjaxResult.error("操作失败");
        }
        for (int i = 0; i < 10; i++) {
            int randomNum = RandomUtil.randomInt(10);
            PriorityDemo data = new PriorityDemo().setName("data-" + i).setOrderNum(randomNum);
            if (QueueUtils.addPriorityQueueObject(queueName, data)) {
                log.info("通道: {} , 发送数据: {}", queueName, data);
            } else {
                log.info("通道: {} , 发送数据: {}, 发送失败", queueName, data);
            }
        }
        return AjaxResult.success("操作成功");
    }

    @ApiOperation("删除队列数据")
    @GetMapping("/remove")
    public AjaxResult<Void> remove(@ApiParam("队列名") String queueName,
                                   @ApiParam("对象名") String name,
                                   @ApiParam("排序号") Integer orderNum) {
        PriorityDemo data = new PriorityDemo().setName(name).setOrderNum(orderNum);
        if (QueueUtils.removePriorityQueueObject(queueName, data)) {
            log.info("通道: {} , 删除数据: {}", queueName, data);
        } else {
            return AjaxResult.error("操作失败");
        }
        return AjaxResult.success("操作成功");
    }

    @ApiOperation("获取队列数据")
    @GetMapping("/get")
    public AjaxResult<Void> get(@ApiParam("队列名") String queueName) {
        PriorityDemo data;
        do {
            data = QueueUtils.getPriorityQueueObject(queueName);
            log.info("通道: {} , 获取数据: {}", queueName, data);
        } while (data != null);
        return AjaxResult.success("操作成功");
    }

}
