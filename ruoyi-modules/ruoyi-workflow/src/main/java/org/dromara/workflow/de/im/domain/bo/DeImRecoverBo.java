package org.dromara.workflow.de.im.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * IM-06 连接失效 / 成员退群 / 暂停解除 / 重新授权 / 消息投递恢复 BO。
 *
 * @author AIP
 */
@Data
public class DeImRecoverBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long channelId;

    /** 恢复动作：reconnect / rejoin / resume / reauth / redeliver */
    @NotBlank
    private String action;
}