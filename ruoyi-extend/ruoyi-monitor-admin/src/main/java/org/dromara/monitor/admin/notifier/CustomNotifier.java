package org.dromara.monitor.admin.notifier;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static de.codecentric.boot.admin.server.domain.values.StatusInfo.*;

/**
 * 自定义事件通知处理
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class CustomNotifier extends AbstractEventNotifier {

    protected CustomNotifier(InstanceRepository repository) {
        super(repository);
    }

    @Override
    @SuppressWarnings("all")
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        return Mono.fromRunnable(() -> {
            // 实例状态改变事件
            if (event instanceof InstanceStatusChangedEvent) {
                // 获取实例注册名称
                String registName = instance.getRegistration().getName();
                // 获取实例ID
                String instanceId = event.getInstance().getValue();
                // 获取实例状态
                String status = ((InstanceStatusChangedEvent) event).getStatusInfo().getStatus();
                // 获取服务URL
                String serviceUrl = instance.getRegistration().getServiceUrl();
                String statusName = null;

                // 根据实例状态执行相应的操作
                switch (status) {
                    // 服务上线（实例成功启动并可以正常处理请求）
                    case STATUS_UP:
                        statusName = "服务上线";
                        break;

                    // 服务离线（实例被手动或自动地从服务中移除）
                    case STATUS_OFFLINE:
                        statusName = "服务离线";
                        break;

                    // 服务受限（表示实例在某些方面受限，可能无法完全提供所有服务）
                    case STATUS_RESTRICTED:
                        statusName = "服务受限";
                        break;

                    // 停止服务状态（表示实例已被标记为停止提供服务，可能是计划内维护或测试）
                    case STATUS_OUT_OF_SERVICE:
                        statusName = "停止服务状态";
                        break;

                    // 服务下线（实例因崩溃、错误或其他原因停止运行）
                    case STATUS_DOWN:
                        statusName = "服务下线";
                        break;

                    // 服务未知异常（监控系统无法确定实例的当前状态）
                    case STATUS_UNKNOWN:
                        statusName = "服务未知异常";
                        break;

                    // 默认情况（没有匹配的状态）
                    default:
                        break;
                }
                log.info("Instance Status Change: 状态名称【{}】, 注册名称【{}】, 实例ID【{}】, 状态【{}】, 服务URL【{}】",
                    statusName, registName, instanceId, status, serviceUrl);
            }
        });
    }
}
