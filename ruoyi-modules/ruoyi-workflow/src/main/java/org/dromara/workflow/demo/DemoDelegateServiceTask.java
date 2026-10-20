package org.dromara.workflow.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.service.task.ServiceTaskContext;
import org.dromara.warm.flow.core.service.task.ServiceTaskHandler;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

/**
 * 服务任务示例：delegateExpression 模式
 * <p>
 * 节点配置写 {@code {"type":"delegateExpression","expression":"#{@demoDelegateServiceTask}"}}，
 * 引擎按 SpEL 从容器取出本 Bean 后调用 {@code execute}。Bean 名即表达式里的名字，改名会打断已发布的定义。
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@Component("demoDelegateServiceTask")
@RequiredArgsConstructor
public class DemoDelegateServiceTask implements ServiceTaskHandler {

    private final DemoOrderService demoOrderService;

    @Override
    public void execute(ServiceTaskContext context) {
        log.info("服务任务[delegateExpression 模式]开始执行: instanceId={}, nodeCode={}",
            context.getInstance().getId(), context.getNode().getNodeCode());
        String orderNo = demoOrderService.createOrder(context);
        context.setVariable("delegateTaskOrderNo", orderNo);
        context.setVariable("delegateTaskResult", StringUtils.format("delegateExpression模式已执行, 单号={}", orderNo));
        log.info("服务任务[delegateExpression 模式]执行完成: instanceId={}, orderNo={}",
            context.getInstance().getId(), orderNo);
    }

}
