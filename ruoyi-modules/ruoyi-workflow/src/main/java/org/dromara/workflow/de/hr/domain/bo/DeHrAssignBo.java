package org.dromara.workflow.de.hr.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * HR-03 责任分派请求 BO。
 * 落点：flow_user(type='1','2','3', processed_by, associated=taskId)
 *
 * @author AIP
 */
@Data
public class DeHrAssignBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long instanceId;

    @NotNull
    private Long taskId;

    /** 候选组 ID 列表 */
    private List<Long> candidateGroupIds;

    /** 岗位 ID（落 flow_user.processed_by，type='1'） */
    private Long postId;

    /** 负载指标（负载均衡参考，落 flow_user.ext 文本或注释；本仓不新造列，故仅记录） */
    private Integer loadFactor;

    /** 代理人 ID（type='3'） */
    private Long delegateId;
}