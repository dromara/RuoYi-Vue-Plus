package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 用户加签查询
 *
 * @author may
 */
@Data
public class SysUserMultiBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
