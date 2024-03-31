package org.dromara.workflow.service;

import org.dromara.workflow.domain.vo.WfDefinitionConfigVo;
import org.dromara.workflow.domain.bo.WfDefinitionConfigBo;

import java.util.Collection;
import java.util.List;

/**
 * 流程定义配置Service接口
 *
 * @author may
 * @date 2024-03-18
 */
public interface IWfDefinitionConfigService {

    /**
     * 查询流程定义配置
     *
     * @param definitionId 流程定义id
     * @return 结果
     */
    WfDefinitionConfigVo getByDefId(String definitionId);

    /**
     * 查询流程定义配置列表
     *
     * @param definitionIds 流程定义id
     * @return 结果
     */
    List<WfDefinitionConfigVo> queryList(List<String> definitionIds);


    /**
     * 新增流程定义配置
     *
     * @param bo 参数
     * @return 结果
     */
    Boolean saveOrUpdate(WfDefinitionConfigBo bo);

    /**
     * 删除
     *
     * @param ids id
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
}
