package org.dromara.monitor.admin.notifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.monitor.admin.event.NotifierEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 信息通知
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class InfoNotifier {

    /**
     * 异步处理通知事件的方法
     * <p>
     * 该方法会处理 `NotifierEvent` 事件，执行通知相关的操作，如发送邮件或短信
     *
     * @param notifier 事件对象，包含了需要通知的详细信息，包括注册名称、状态名称、实例 ID、状态和服务 URL
     */
    @Async
    @EventListener
    public void infoNotification(NotifierEvent notifier) {
        // 在这里添加处理通知事件的逻辑
        // 例如，依据 notifier 对象的信息发送邮件或短信通知
    }

}
