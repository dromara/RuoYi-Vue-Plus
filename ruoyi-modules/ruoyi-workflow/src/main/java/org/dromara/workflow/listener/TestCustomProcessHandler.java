package org.dromara.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.annotation.FlowListenerAnnotation;
import org.dromara.workflow.flowable.strategy.FlowProcessEventHandler;
import org.springframework.stereotype.Component;

/**
 * 自定义监听测试
 *
 * @author may
 * @date 2023-12-27
 */
@Slf4j
@Component
@FlowListenerAnnotation(processDefinitionKey = "leave1")
public class TestCustomProcessHandler implements FlowProcessEventHandler {


    /**
     * 执行办理任务监听
     *
     * @param businessKey 业务id
     * @param status      状态
     * @param submit      当为true时为申请人节点办理
     */
    @Override
    public void handleProcess(String businessKey, String status, boolean submit) {
        log.info("业务ID:" + businessKey + ",状态:" + status);
    }
}
