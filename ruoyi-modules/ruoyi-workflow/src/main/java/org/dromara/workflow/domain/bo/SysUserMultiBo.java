package org.dromara.workflow.domain.bo;

import lombok.Data;
import org.dromara.workflow.common.PageEntity;


/**
 * 用户加签查询
 *
 * @author may
 */
@Data

public class SysUserMultiBo extends PageEntity {
    /**
     * 人员名称
     */
    private String userName;

    /**
     * 人员名称
     */
    private String nickName;

    /**
     * 部门id
     */
    private String deptId;

    /**
     * 任务id
     */
    private String taskId;
}
