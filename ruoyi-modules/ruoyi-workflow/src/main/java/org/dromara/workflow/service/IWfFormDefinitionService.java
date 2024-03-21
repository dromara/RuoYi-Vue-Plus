package org.dromara.workflow.service;

import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.domain.bo.WfFormDefinitionBo;

import java.util.Collection;
import java.util.List;

/**
 * 表单配置Service接口
 *
 * @author gssong
 * @date 2024-03-18
 */
public interface IWfFormDefinitionService {

    /**
     * 查询表单配置
     *
     * @param definitionId 流程定义id
     * @return 结果
     */
    WfFormDefinitionVo getByDefId(String definitionId);

    /**
     * 查询表单配置列表
     *
     * @param definitionIds 流程定义id
     * @return 结果
     */
    List<WfFormDefinitionVo> queryList(List<String> definitionIds);


    /**
     * 新增表单配置
     *
     * @param bo 参数
     * @return 结果
     */
    Boolean saveOrUpdate(WfFormDefinitionBo bo);

    /**
     * 删除
     *
     * @param ids id
     * @return 结果
     */
    Boolean deleteByIds(Collection<Long> ids);
}
