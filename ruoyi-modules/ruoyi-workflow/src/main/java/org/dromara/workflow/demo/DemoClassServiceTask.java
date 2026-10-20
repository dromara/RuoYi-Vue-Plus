package org.dromara.workflow.demo;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.service.task.ServiceTaskContext;
import org.dromara.warm.flow.core.service.task.ServiceTaskHandler;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

/**
 * 服务任务示例：class 模式
 * <p>
 * 节点配置 {@code node_config.serviceTask} 里写 {@code {"type":"class","handler":"本类全名"}}，
 * 引擎先按全名从容器取 Bean，取不到才反射新建实例，所以注册成 Bean 后可以注入其它服务。
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@Component
public class DemoClassServiceTask implements ServiceTaskHandler {

    @Override
    public void execute(ServiceTaskContext context) {
        log.info("服务任务[class 模式]开始执行: instanceId={}, nodeCode={}, nodeName={}",
            context.getInstance().getId(), context.getNode().getNodeCode(), context.getNode().getNodeName());
        context.setVariable("classTaskNode", context.getNode().getNodeCode());
        context.setVariable("classTaskResult", "class模式已执行");
        log.info("服务任务[class 模式]执行完成: instanceId={}, nodeCode={}",
            context.getInstance().getId(), context.getNode().getNodeCode());
    }

}
