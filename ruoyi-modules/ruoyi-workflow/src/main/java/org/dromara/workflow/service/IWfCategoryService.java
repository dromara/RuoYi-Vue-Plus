package org.dromara.workflow.service;

import org.dromara.workflow.domain.WfCategory;
import org.dromara.workflow.domain.bo.WfCategoryBo;
import org.dromara.workflow.domain.vo.WfCategoryVo;

import java.util.Collection;
import java.util.List;

/**
 * 流程分类Service接口
 *
 * @author may
 * @date 2023-06-28
 */
public interface IWfCategoryService {

    /**
     * 查询流程分类
     */
    WfCategoryVo queryById(Long id);


    /**
     * 查询流程分类列表
     */
    List<WfCategoryVo> queryList(WfCategoryBo bo);

    /**
     * 新增流程分类
     */
    Boolean insertByBo(WfCategoryBo bo);

    /**
     * 修改流程分类
     */
    Boolean updateByBo(WfCategoryBo bo);

    /**
     * 校验并批量删除流程分类信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 按照类别编码查询
     *
     * @param categoryCode 分类比吗
     * @return 结果
     */
    WfCategory queryByCategoryCode(String categoryCode);
}
