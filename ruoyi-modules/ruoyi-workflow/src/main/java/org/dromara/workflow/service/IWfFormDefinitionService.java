package org.dromara.workflow.service;

import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.domain.bo.WfFormDefinitionBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 动态单与流程定义关联信息Service接口
 *
 * @author may
 * @date 2023-08-31
 */
public interface IWfFormDefinitionService {

    /**
     * 查询动态单与流程定义关联信息
     */
    WfFormDefinitionVo queryById(Long id);

    /**
     * 查询动态单与流程定义关联信息列表
     */
    TableDataInfo<WfFormDefinitionVo> queryPageList(WfFormDefinitionBo bo, PageQuery pageQuery);

    /**
     * 查询动态单与流程定义关联信息列表
     */
    List<WfFormDefinitionVo> queryList(WfFormDefinitionBo bo);

    /**
     * 新增动态单与流程定义关联信息
     */
    Boolean insertByBo(WfFormDefinitionBo bo);

    /**
     * 修改动态单与流程定义关联信息
     */
    Boolean updateByBo(WfFormDefinitionBo bo);

    /**
     * 校验并批量删除动态单与流程定义关联信息信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
