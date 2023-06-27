package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.WfFormBo;
import org.dromara.workflow.domain.vo.WfFormVo;

import java.util.Collection;
import java.util.List;

/**
 * 流程表单 服务层
 *
 * @author KonBAI
 */
public interface IWfFormService {
    /**
     * 查询流程表单
     *
     * @param formId 流程表单ID
     * @return 流程表单
     */
    WfFormVo queryById(Long formId);

    /**
     * 查询流程表单列表
     *
     * @param bo 流程表单
     * @return 流程表单集合
     */
    TableDataInfo<WfFormVo> queryPageList(WfFormBo bo, PageQuery pageQuery);

    /**
     * 查询流程表单列表
     *
     * @param bo 流程表单
     * @return 流程表单集合
     */
    List<WfFormVo> queryList(WfFormBo bo);

    /**
     * 新增流程表单
     *
     * @param bo 流程表单
     * @return 结果
     */
    int insertForm(WfFormBo bo);

    /**
     * 修改流程表单
     *
     * @param bo 流程表单
     * @return 结果
     */
    int updateForm(WfFormBo bo);

    /**
     * 批量删除流程表单
     *
     * @param formIds 需要删除的流程表单ID
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> formIds);
}
