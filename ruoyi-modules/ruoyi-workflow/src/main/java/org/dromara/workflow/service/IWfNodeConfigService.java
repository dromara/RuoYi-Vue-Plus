package org.dromara.workflow.service;

import org.dromara.workflow.domain.WfNodeConfig;
import org.dromara.workflow.domain.vo.WfNodeConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 节点配置Service接口
 *
 * @author may
 * @date 2024-03-30
 */
public interface IWfNodeConfigService {

    /**
     * 查询节点配置
     *
     * @param id 主键
     * @return 结果
     */
    WfNodeConfigVo queryById(Long id);

    /**
     * 保存节点配置
     *
     * @param list 参数
     * @return 结果
     */
    Boolean saveOrUpdate(List<WfNodeConfig> list);

    /**
     * 批量删除节点配置信息
     *
     * @param ids 主键
     * @return 结果
     */
    Boolean deleteByIds(Collection<Long> ids);

    /**
     * 按照流程定义id删除
     *
     * @param ids 流程定义id
     * @return 结果
     */
    Boolean deleteByDefIds(Collection<String> ids);

    /**
     * 按照流程定义id查询
     *
     * @param ids 流程定义id
     * @return 结果
     */
    List<WfNodeConfigVo> selectByDefIds(Collection<String> ids);
}
