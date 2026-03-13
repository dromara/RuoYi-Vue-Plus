package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.bo.FlowCancelBo;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.domain.bo.FlowInvalidBo;
import org.dromara.workflow.domain.bo.FlowVariableBo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;

import java.util.List;
import java.util.Map;

/**
 * 流程实例 服务层
 *
 * @author may
 */
public interface IFlwInstanceService {

    /**
     * 分页查询正在运行的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> selectRunningInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery);

    /**
     * 分页查询已结束的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> selectFinishInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery);

    /**
     * 根据业务 id 查询流程实例详情，返回业务侧展示所需信息。
     *
     * @param businessId 业务id
     * @return 结果
     */
    FlowInstanceVo queryByBusinessId(Long businessId);

    /**
     * 按业务 id 查询底层流程实例实体。
     *
     * @param businessId 业务id
     * @return 结果
     */
    FlowInstance selectInstByBusinessId(String businessId);

    /**
     * 按实例 id 查询单个流程实例实体。
     *
     * @param instanceId 实例id
     * @return 结果
     */
    FlowInstance selectInstById(Long instanceId);

    /**
     * 按实例 id 集合批量查询流程实例。
     *
     * @param instanceIds 实例id
     * @return 结果
     */
    List<FlowInstance> selectInstListByIdList(List<Long> instanceIds);

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    boolean deleteByBusinessIds(List<String> businessIds);

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     * @return 结果
     */
    boolean deleteByInstanceIds(List<Long> instanceIds);

    /**
     * 按照实例id删除已完成得流程实例
     *
     * @param instanceIds 删除的实例id
     * @return 删除结果
     */
    boolean deleteHisByInstanceIds(List<Long> instanceIds);

    /**
     * 撤销流程
     *
     * @param bo 参数
     * @return 结果
     */
    boolean cancelProcessApply(FlowCancelBo bo);

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 流程实例
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> selectCurrentInstanceList(FlowInstanceBo instanceBo, PageQuery pageQuery);

    /**
     * 获取流程图及历史办理记录，供前端展示流转轨迹。
     *
     * @param businessId 业务id
     * @return 结果
     */
    Map<String, Object> flowHisTaskList(String businessId);

    /**
     * 按照实例id更新状态
     *
     * @param instanceId 实例id
     * @param status     状态
     */
    void updateStatus(Long instanceId, String status);

    /**
     * 获取指定实例当前持有的流程变量。
     *
     * @param instanceId 实例id
     * @return 结果
     */
    Map<String, Object> instanceVariable(Long instanceId);

    /**
     * 更新流程变量，支持在运行中修正流程上下文数据。
     *
     * @param bo 参数
     * @return 结果
     */
    boolean updateVariable(FlowVariableBo bo);

    /**
     * 为指定流程实例设置变量集合。
     *
     * @param instanceId 实例id
     * @param variable   流程变量
     */
    void setVariable(Long instanceId, Map<String, Object> variable);

    /**
     * 按任务 id 反查所属流程实例。
     *
     * @param taskId 任务id
     * @return 结果
     */
    FlowInstance selectByTaskId(Long taskId);

    /**
     * 作废流程
     *
     * @param bo 流程实例
     * @return 结果
     */
    boolean processInvalid(FlowInvalidBo bo);
}
