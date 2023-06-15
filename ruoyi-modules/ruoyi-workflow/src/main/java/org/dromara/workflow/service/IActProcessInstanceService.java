package org.dromara.workflow.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.bo.ProcessInvalidBo;
import org.dromara.workflow.domain.vo.ActHistoryInfoVo;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;

import java.util.List;
import java.util.Map;

/**
 * 流程实例 服务层
 *
 * @author may
 */
public interface IActProcessInstanceService {
    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     * @param response          响应
     */
    void getHistoryProcessImage(String processInstanceId, HttpServletResponse response);

    /**
     * 分页查询正在运行的流程实例
     *
     * @param processInstanceBo 参数
     * @return 结果
     */
    TableDataInfo<ProcessInstanceVo> getProcessInstanceRunningByPage(ProcessInstanceBo processInstanceBo);

    /**
     * 分页查询已结束的流程实例
     *
     * @param processInstanceBo 参数
     * @return 结果
     */
    TableDataInfo<ProcessInstanceVo> getProcessInstanceFinishByPage(ProcessInstanceBo processInstanceBo);

    /**
     * 获取审批记录
     *
     * @param processInstanceId 流程实例id
     * @return 结果
     */
    Map<String, Object> getHistoryRecord(String processInstanceId);

    /**
     * 作废流程实例，不会删除历史记录(删除运行中的实例)
     *
     * @param processInvalidBo 参数
     * @return 结果
     */
    boolean deleteRuntimeProcessInst(ProcessInvalidBo processInvalidBo);

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceId 流程实例id
     * @return 结果
     */
    boolean deleteRuntimeProcessAndHisInst(String processInstanceId);

    /**
     * 已完成的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceId 流程实例id
     * @return 结果
     */
    boolean deleteFinishProcessAndHisInst(String processInstanceId);
}
