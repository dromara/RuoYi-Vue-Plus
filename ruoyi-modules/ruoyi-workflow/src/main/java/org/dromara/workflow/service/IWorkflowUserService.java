package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.workflow.domain.bo.SysUserMultiBo;
import org.dromara.workflow.domain.vo.TaskVo;

import java.util.List;

/**
 * 工作流用户选人管理 服务层
 *
 * @author may
 */
public interface IWorkflowUserService {

    /**
     * 分页查询工作流选择加签人员
     *
     * @param sysUserMultiBo 参数
     * @return 结果
     */
    TableDataInfo<SysUserVo> getWorkflowAddMultiInstanceByPage(SysUserMultiBo sysUserMultiBo);

    /**
     * 查询工作流选择减签人员
     *
     * @param taskId 任务id
     * @return 结果
     */
    List<TaskVo> getWorkflowDeleteMultiInstanceList(String taskId);
}
