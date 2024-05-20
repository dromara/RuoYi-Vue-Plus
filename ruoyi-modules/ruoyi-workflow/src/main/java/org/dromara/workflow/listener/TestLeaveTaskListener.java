package org.dromara.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 流程任务监听测试
 *
 * @author may
 * @date 2023-12-12
 */
@Slf4j
@Component("testLeaveTaskListener")
public class TestLeaveTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("执行监听【" + delegateTask.getName() + "】");
    }
}
