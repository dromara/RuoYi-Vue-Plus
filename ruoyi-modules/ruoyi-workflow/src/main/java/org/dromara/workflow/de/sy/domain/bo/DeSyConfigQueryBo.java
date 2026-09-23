package org.dromara.workflow.de.sy.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SY-02 系统配置查询 BO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeSyConfigQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String configName;
    private String configKey;
    private String configType;
}