package org.dromara.workflow.de.hc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * HC-04 协作请求 BO。
 *
 * @author AIP
 */
@Data
public class DeHcCollabRequestBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long fromUserId;

    @NotNull
    private Long toUserId;

    @NotBlank
    private String target;

    /** 输入 / 交付 / 期限 / 范围 自由文本 */
    private String inputSpec;
    private String deliverySpec;
    private String dueAt;
    private String scope;
}