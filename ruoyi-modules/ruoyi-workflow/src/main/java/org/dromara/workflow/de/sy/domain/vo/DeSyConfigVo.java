package org.dromara.workflow.de.sy.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * SY-02 系统配置响应 VO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeSyConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long configId;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private Long createDept;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
    private String remark;
}