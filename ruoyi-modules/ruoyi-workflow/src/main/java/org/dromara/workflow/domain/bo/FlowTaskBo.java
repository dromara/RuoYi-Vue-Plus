package org.dromara.workflow.domain.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务请求对象
 *
 * @author may
 */
@Data
public class FlowTaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    private String nodeName;

    /**
     * 流程定义名称
     */
    private String flowName;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 流程分类id
     */
    private String category;

    /**
     * 流程实例id
     */
    private Long instanceId;

    /**
     * 流程状态
     */
    private String flowStatus;

    /**
     * 权限列表
     */
    private List<String> permissionList;

    /**
     * 申请人Ids
     */
    private List<Long> createByIds;

    /**
     * 请求参数
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params = new HashMap<>();

}
