package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程定义表 flow_definition 实体（DDL 真值 04-backend-code/script/sql/ry_workflow.sql:4）。
 *
 * <p>BR-17 「model_value 模板继承」的来源表。仅暴露 4 列。
 */
@Data
@TableName("flow_definition")
public class FlowDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 流程编码 */
    private String flowCode;

    /** 流程名称 */
    private String flowName;

    /** 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）—— BR-17 核心列 */
    private String modelValue;
}
