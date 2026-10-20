package org.dromara.workflow.demo;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.service.task.ServiceTaskContext;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务任务示例：expression 模式的被调用方
 * <p>
 * 节点配置写 {@code {"type":"expression","expression":"#{@demoOrderService.createOrder(#execution)}",
 * "resultVariable":"orderNo"}}，{@code #execution} 就是引擎传入的 {@link ServiceTaskContext}，
 * 返回值按 {@code resultVariable} 写回流程变量。
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@Component("demoOrderService")
public class DemoOrderService {

    /**
     * 示例单号序列，仅用于演示“服务任务产出变量供后续网关判断”，非业务落库。
     */
    private final AtomicLong sequence = new AtomicLong(1000L);

    /**
     * 生成示例业务单号。
     *
     * @param context 服务任务上下文
     * @return 示例单号
     */
    public String createOrder(ServiceTaskContext context) {
        log.info("服务任务[expression 模式]开始执行: instanceId={}, nodeCode={}",
            context.getInstance().getId(), context.getNode().getNodeCode());
        context.setVariable("expressionTaskNode", context.getNode().getNodeCode());
        context.setVariable("expressionTaskResult", "expression模式已执行");
        String orderNo = "SO" + sequence.incrementAndGet();
        log.info("服务任务[expression 模式]执行完成: instanceId={}, nodeCode={}, orderNo={}",
            context.getInstance().getId(), context.getNode().getNodeCode(), orderNo);
        return orderNo;
    }

    /**
     * 按入参变量算请假天数，产出变量供后面的网关条件（如 {@code gt@@day|3}）判断。
     *
     * @param context 服务任务上下文
     * @return 天数
     */
    public Integer evalDay(ServiceTaskContext context) {
        Object inputDay = context.getVariable("inputDay");
        int day = inputDay == null ? 5 : Integer.parseInt(String.valueOf(inputDay));
        log.info("服务任务[expression 模式]计算天数: instanceId={}, nodeCode={}, inputDay={}, day={}",
            context.getInstance().getId(), context.getNode().getNodeCode(), inputDay, day);
        return day;
    }

}
