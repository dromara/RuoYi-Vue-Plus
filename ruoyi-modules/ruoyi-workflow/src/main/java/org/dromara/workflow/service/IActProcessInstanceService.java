package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.bo.ProcessInvalidBo;
import org.dromara.workflow.domain.bo.TaskUrgingBo;
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
     * @param businessKey 流程实例id
     * @return 结果
     */
    String getHistoryImage(String businessKey);

    /**
     * 通过业务id获取历史流程图运行中，历史等节点
     *
     * @param businessKey 业务id
     * @return 结果
     */
    Map<String, Object> getHistoryList(String businessKey);

    /**
     * 分页查询正在运行的流程实例
     *
     * @param processInstanceBo 参数
     * @param pageQuery         分页
     * @return 结果
     */
    TableDataInfo<ProcessInstanceVo> getPageByRunning(ProcessInstanceBo processInstanceBo, PageQuery pageQuery);

    /**
     * 分页查询已结束的流程实例
     *
     * @param processInstanceBo 参数
     * @param pageQuery         分页
     * @return 结果
     */
    TableDataInfo<ProcessInstanceVo> getPageByFinish(ProcessInstanceBo processInstanceBo, PageQuery pageQuery);

    /**
     * 获取审批记录
     *
     * @param businessKey 业务id
     * @return 结果
     */
    List<ActHistoryInfoVo> getHistoryRecord(String businessKey);

    /**
     * 作废流程实例，不会删除历史记录(删除运行中的实例)
     *
     * @param processInvalidBo 参数
     * @return 结果
     */
    boolean deleteRunInstance(ProcessInvalidBo processInvalidBo);

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    boolean deleteRunAndHisInstance(List<String> businessKeys);

    /**
     * 已完成的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    boolean deleteFinishAndHisInstance(List<String> businessKeys);

    /**
     * 撤销流程申请
     *
     * @param businessKey 业务id
     * @return 结果
     */
    boolean cancelProcessApply(String businessKey);

    /**
     * 分页查询当前登录人单据
     *
     * @param processInstanceBo 参数
     * @param pageQuery         分页
     * @return 结果
     */
    TableDataInfo<ProcessInstanceVo> getPageByCurrent(ProcessInstanceBo processInstanceBo, PageQuery pageQuery);

    /**
     * 任务催办(给当前任务办理人发送站内信，邮件，短信等)
     *
     * @param taskUrgingBo 任务催办
     * @return 结果
     */
    boolean taskUrging(TaskUrgingBo taskUrgingBo);
}
