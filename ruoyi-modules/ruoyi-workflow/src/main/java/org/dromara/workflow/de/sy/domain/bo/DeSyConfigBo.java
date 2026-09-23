package org.dromara.workflow.de.sy.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SY-02 系统配置 CRUD 请求 BO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeSyConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 参数主键（更新时必填） */
    private Long configId;

    /** 参数名称（必填） */
    @NotBlank
    private String configName;

    /** 参数键名（必填） */
    @NotBlank
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 系统内置（Y是 N否） */
    private String configType;

    /** 备注 */
    private String remark;
}