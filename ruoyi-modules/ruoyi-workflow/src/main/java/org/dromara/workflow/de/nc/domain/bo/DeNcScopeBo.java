package org.dromara.workflow.de.nc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * NC-05 节点配置保存范围 BO。
 *
 * @author AIP
 */
@Data
public class DeNcScopeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long nodeId;

    /** 范围类型：node-only / personal / shared-template */
    @NotBlank
    private String scope;

    /** 配置 JSON 字符串（落到 flow_node.ext 列） */
    private String config;
}