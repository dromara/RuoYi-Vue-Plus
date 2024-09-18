package org.dromara.workflow.domain.vo;

import com.warm.flow.core.entity.User;
import lombok.Data;
import org.dromara.common.core.domain.dto.UserDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 任务视图
 *
 * @author may
 */
@Data
public class FlowTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 删除标记
     */
    private String delFlag;

    /**
     * 对应flow_definition表的id
     */
    private Long definitionId;

    /**
     * 流程实例表id
     */
    private Long instanceId;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 权限标识 permissionFlag的list形式
     */
    private List<String> permissionList;

    /**
     * 流程用户列表
     */
    private List<User> userList;

    /**
     * 审批表单是否自定义（Y是 2否）
     */
    private String formCustom;

    /**
     * 审批表单
     */
    private String formPath;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 流程版本号
     */
    private String version;

    /**
     * 流程状态
     */
    private String flowStatus;

    /**
     * 流程状态
     */
    private String flowStatusName;

    /**
     * 办理人
     */
    private List<UserDTO> userDTOList;
}
