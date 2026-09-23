package org.dromara.workflow.de.hc.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * HC-03 三分类位落点请求 BO（2026-09-23）。
 *
 * <p>落点：flow_node.permission_flag（varchar 200 + @@ 分隔 + : 前缀；DDL 自带「多权限」注释）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeHcThreeCategoryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点 ID（必填） */
    @NotNull
    private Long nodeId;

    /** 三分类位字符串（拼装好直接写 permission_flag；可选） */
    private String permissionFlag;

    /** COPY 段（用户列表，逗号分隔） */
    private String copyUsers;

    /** COOPERATE 段（角色列表，逗号分隔） */
    private String cooperateRoles;

    /** DELEGATE 段（部门列表，逗号分隔） */
    private String delegateDepts;
}