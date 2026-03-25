package org.dromara.workflow.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 抄送对象
 *
 * @author AprilWind
 */
@Data
public class FlowCopyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "userId")
    private String nickName;

    public FlowCopyVo(Long userId) {
        this.userId = userId;
    }

}
