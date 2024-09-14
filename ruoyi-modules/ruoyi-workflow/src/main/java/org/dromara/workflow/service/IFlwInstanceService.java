package org.dromara.workflow.service;

import com.warm.flow.core.entity.Instance;
import com.warm.flow.orm.entity.FlowInstance;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.InstanceBo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;

import java.util.List;

/**
 * 流程实例 服务层
 *
 * @author may
 */
public interface IFlwInstanceService {

    /**
     * 分页查询正在运行的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> getPageByRunning(Instance instance, PageQuery pageQuery);

    /**
     * 分页查询已结束的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> getPageByFinish(Instance instance, PageQuery pageQuery);

    /**
     * 按照业务id查询流程实例
     *
     * @param businessId 业务id
     * @return 结果
     */
    FlowInstance instanceByBusinessId(String businessId);

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    boolean deleteByBusinessIds(List<Long> businessIds);

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     * @return 结果
     */
    boolean deleteByInstanceIds(List<Long> instanceIds);

    /**
     * 撤销流程
     *
     * @param businessId 业务id
     * @return 结果
     */
    boolean cancelProcessApply(String businessId);

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowInstanceVo> getPageByCurrent(InstanceBo instanceBo, PageQuery pageQuery);
}
