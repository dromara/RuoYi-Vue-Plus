package org.dromara.workflow.demo;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.spi.Listener;
import org.dromara.warm.flow.core.spi.ListenerVariable;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 付款失败善后监听器（示例）
 * <p>
 * 挂在「财务执行付款」节点的 create 监听上：付款金额超过限额就调 {@code TaskService#throwError}
 * 抛出 {@code PAY_FAILED}，由流程定义上的错误边界捕获并走善后分支。金额先取本次办理变量，
 * 取不到再回落实例变量，因为监听器可能在变量合并前触发。
 * <p>
 * 节点监听器配置里写的是本类全名，改包名或类名要同步改已发布的流程定义。
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@Component
public class DemoPayFailListener implements Listener {

    /**
     * 付款金额变量名。
     */
    public static final String VAR_PAY_AMOUNT = "payAmount";

    /**
     * 免审限额，超过即视为付款失败需善后。
     */
    public static final BigDecimal PAY_AMOUNT_LIMIT = new BigDecimal("1000");

    /**
     * 错误码，需与流程定义上错误边界的错误码一致。
     */
    public static final String ERROR_CODE_PAY_FAILED = "PAY_FAILED";

    @Override
    public void notify(ListenerVariable listenerVariable) {
        Task task = listenerVariable == null ? null : listenerVariable.getTask();
        if (task == null || task.getId() == null) {
            return;
        }
        BigDecimal amount = readPayAmount(listenerVariable);
        if (amount == null || amount.compareTo(PAY_AMOUNT_LIMIT) <= 0) {
            return;
        }
        log.info("付款金额 {} 超过限额 {}，抛出 {} 转入错误边界: instanceId={}, taskId={}",
            amount, PAY_AMOUNT_LIMIT, ERROR_CODE_PAY_FAILED, task.getInstanceId(), task.getId());
        FlowEngine.taskService().throwError(task.getId(), ERROR_CODE_PAY_FAILED,
            "付款金额 " + amount.toPlainString() + " 超过限额 " + PAY_AMOUNT_LIMIT.toPlainString() + "，转入善后处理");
    }

    /**
     * 先取本次办理的变量，取不到再回落实例变量。
     *
     * @param listenerVariable 监听器变量
     * @return 付款金额，未配置时返回 {@code null}
     */
    private BigDecimal readPayAmount(ListenerVariable listenerVariable) {
        BigDecimal amount = parseAmount(listenerVariable.getVariable());
        if (amount != null) {
            return amount;
        }
        Instance instance = listenerVariable.getInstance();
        return parseAmount(instance == null ? null : instance.getVariableMap());
    }

    /**
     * 从变量集合里解析金额，非法值按未配置处理。
     *
     * @param variable 变量集合
     * @return 付款金额，解析失败返回 {@code null}
     */
    private BigDecimal parseAmount(Map<String, Object> variable) {
        if (variable == null || variable.get(VAR_PAY_AMOUNT) == null) {
            return null;
        }
        String value = String.valueOf(variable.get(VAR_PAY_AMOUNT));
        if (!StringUtils.isNumeric(value)) {
            log.warn("流程变量 {} 不是数字，跳过付款校验: {}", VAR_PAY_AMOUNT, value);
            return null;
        }
        return new BigDecimal(value);
    }

}
