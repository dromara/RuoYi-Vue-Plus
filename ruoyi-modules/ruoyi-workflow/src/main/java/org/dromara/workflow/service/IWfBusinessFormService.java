package org.dromara.workflow.service;

import org.dromara.workflow.domain.WfBusinessForm;
import org.dromara.workflow.domain.vo.WfBusinessFormVo;
import org.dromara.workflow.domain.bo.WfBusinessFormBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 发起流程Service接口
 *
 * @author may
 * @date 2023-09-16
 */
public interface IWfBusinessFormService {

    /**
     * 查询发起流程
     */
    WfBusinessFormVo queryById(Long id);

    /**
     * 查询发起流程列表
     */
    TableDataInfo<WfBusinessFormVo> queryPageList(WfBusinessFormBo bo, PageQuery pageQuery);

    /**
     * 查询发起流程列表
     */
    List<WfBusinessFormVo> queryList(WfBusinessFormBo bo);

    /**
     * 新增发起流程
     */
    WfBusinessForm insertByBo(WfBusinessFormBo bo);

    /**
     * 修改发起流程
     */
    WfBusinessForm updateByBo(WfBusinessFormBo bo);

    /**
     * 校验并批量删除发起流程信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);
}
