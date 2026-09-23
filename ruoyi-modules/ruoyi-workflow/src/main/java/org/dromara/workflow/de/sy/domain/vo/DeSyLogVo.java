package org.dromara.workflow.de.sy.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * SY-03 系统操作日志响应 VO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeSyLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long operId;
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private Integer operatorType;
    private String operName;
    private Long userId;
    private Long deptId;
    private String deptName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private Integer status;
    private String errorMsg;
    private Date operTime;
    private Long costTime;
}