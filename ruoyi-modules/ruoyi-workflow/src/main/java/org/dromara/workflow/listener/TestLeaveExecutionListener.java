package org.dromara.workflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.utils.QueryUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * 流程实例监听测试
 *
 * @author may
 * @date 2023-12-12
 */
@Slf4j
@RequiredArgsConstructor
@Component("testLeaveExecutionListener")
public class TestLeaveExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        Task task = QueryUtils.taskQuery().executionId(execution.getId()).singleResult();
        log.info("执行监听【" + task.getName() + "】");
    }
}
