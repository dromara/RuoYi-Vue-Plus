package org.dromara.workflow.de.sy.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * SY-06 人类纠正 BO。
 *
 * @author AIP
 */
@Data
public class DeSyCorrectionBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    /** 纠正摘要文本 */
    private String summary;

    /** 与摘要冲突的事实来源 */
    private String conflictingFact;
}