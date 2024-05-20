package org.dromara.workflow.service;


import org.dromara.workflow.domain.ActHiProcinst;

import java.util.List;

/**
 * 流程实例Service接口
 *
 * @author may
 * @date 2023-07-22
 */
public interface IActHiProcinstService {

    /**
     * 按照业务id查询
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    List<ActHiProcinst> selectByBusinessKeyIn(List<String> businessKeys);

    /**
     * 按照业务id查询
     *
     * @param businessKey 业务id
     * @return 结果
     */
    ActHiProcinst selectByBusinessKey(String businessKey);
}
