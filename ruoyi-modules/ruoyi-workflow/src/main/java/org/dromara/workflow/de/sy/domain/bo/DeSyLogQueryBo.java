package org.dromara.workflow.de.sy.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SY-03 系统操作日志查询 BO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeSyLogQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String title;
    private String operName;
    private Integer businessType;
    private Integer status;
    private String operIp;
}