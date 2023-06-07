package org.dromara.workflow.service;

import org.dromara.workflow.domain.bo.StartProcessBo;

import java.util.Map;

/**
 * 任务 服务层
 *
 * @author may
 */
public interface IActTaskService {
    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     * @return 结果
     */
    Map<String, Object> startWorkFlow(StartProcessBo startProcessBo);
}
