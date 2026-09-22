package org.dromara.workflow.de.im.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * IM-04 真人专家咨询请求 BO。
 *
 * @author AIP
 */
@Data
public class DeImExpertConsultBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    @NotNull
    private Long expertUserId;

    /** 咨询议题 */
    private String question;
}