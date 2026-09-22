package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程跳转表 flow_skip 实体（DDL 真值 04-backend-code/script/sql/ry_workflow.sql:54）。
 *
 * <p>BR-15 / BR-16 / BR-18 「skip_type」的来源表。仅暴露 5 列。
 */
@Data
@TableName("flow_skip")
public class FlowSkip implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 流程定义 ID */
    private Long definitionId;

    /** 当前节点编码 */
    private String nowNodeCode;

    /** 当前节点类型 */
    private Integer nowNodeType;

    /** 下一个节点编码 */
    private String nextNodeCode;

    /** 下一个节点类型 */
    private Integer nextNodeType;

    /** 跳转类型（PASS审批通过 REJECT退回）—— BR-15/16/18 核心列 */
    private String skipType;
}
