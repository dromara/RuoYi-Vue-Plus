package org.dromara.workflow.de.hc.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * HC-03 三分类位落点响应 VO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeHcThreeCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点 ID */
    private Long nodeId;

    /** permission_flag 原值（DDL varchar(200) + @@ 分隔 + : 前缀） */
    private String permissionFlag;

    /** 解析后的三段 */
    private List<String> copyUsers;
    private List<String> cooperateRoles;
    private List<String> delegateDepts;
}