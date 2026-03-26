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

    private final PushSessionManager pushSessionManager;

    @Override
    public void run(ApplicationArguments args) {
        pushSessionManager.subscribeMessage(message -> {
            log.info("消息主题订阅收到消息userIds={} message={}",
                message.getUserIds(),
                message.getPayload() == null ? null : message.getPayload().getMessage());
            if (message.getPayload() == null) {
                return;
            }
            if (CollUtil.isNotEmpty(message.getUserIds())) {
                message.getUserIds().forEach(userId -> pushSessionManager.sendMessage(userId, message.getPayload()));
            } else {
                pushSessionManager.sendMessage(message.getPayload());
            }
        });
        log.info("初始化消息主题订阅监听器成功");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
