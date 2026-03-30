package org.dromara.common.push.listener;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.push.core.PushSessionManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

/**
 * 统一消息主题订阅监听器。
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
public class MessageTopicListener implements ApplicationRunner, Ordered {

    /**
     * 推送会话管理器
     */
    private final PushSessionManager pushSessionManager;

    /**
     * 项目启动后执行
     * 注册消息订阅，监听消息并分发给对应用户/全局广播
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // 订阅消息主题，处理消息分发
        pushSessionManager.subscribeMessage(message -> {
            log.info("消息主题订阅收到消息userIds={} message={}",
                message.getUserIds(),
                message.getPayload() == null ? null : message.getPayload().getMessage());
            if (message.getPayload() == null) {
                return;
            }
            // 有指定用户 -> 单发
            if (CollUtil.isNotEmpty(message.getUserIds())) {
                message.getUserIds().forEach(userId -> pushSessionManager.sendMessage(userId, message.getPayload()));
            } else {
                // 无指定用户 -> 全局广播
                pushSessionManager.sendMessage(message.getPayload());
            }
        });
        log.info("初始化消息主题订阅监听器成功");
    }

    /**
     * 执行顺序，优先级设为最高，确保消息订阅最先初始化
     *
     * @return 优先级，值越小越先执行
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
