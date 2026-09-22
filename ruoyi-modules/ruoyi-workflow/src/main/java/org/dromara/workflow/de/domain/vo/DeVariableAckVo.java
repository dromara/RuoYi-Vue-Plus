package org.dromara.workflow.de.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * variable 更新响应 VO。
 *
 * @author AIP
 */
@Data
public class DeVariableAckVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 写入的 instance_id */
    private Long instanceId;

    /** 写入后的 de.ctx 顶层键路径 */
    private String keyPath;

    /** 是否经过应用层形状校验（true 必写 / false 拒收） */
    private Boolean validated;

    /** 校验失败原因（validated=true 时为 null） */
    private String rejectReason;
}