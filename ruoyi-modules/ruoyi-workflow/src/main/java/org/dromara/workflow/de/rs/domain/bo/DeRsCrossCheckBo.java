package org.dromara.workflow.de.rs.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * RS-06 外部系统写入结果与提交产物交叉核验 BO。
 *
 * @author AIP
 */
@Data
public class DeRsCrossCheckBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    /** key = ossId, value = 期望 hash / 业务标识 */
    private Map<String, String> expected;
}