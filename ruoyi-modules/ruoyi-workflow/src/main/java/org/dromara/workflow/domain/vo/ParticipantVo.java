package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 参与者
 *
 * @author may
 */
@Data
public class ParticipantVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组id（角色id）
     */
    private List<Long> groupIds;

    /**
     * 候选人id（用户id） 当组id不为空时，将组内人员查出放入candidate
     */
    private List<Long> candidate;

    /**
     * 候选人名称（用户名称） 当组id不为空时，将组内人员查出放入candidateName
     */
    private List<String> candidateName;

    /**
     * 是否认领标识
     * 当为空时默认当前任务不需要认领
     * 当为true时当前任务说明为候选模式并且有人已经认领了任务可以归还，
     * 当为false时当前任务说明为候选模式该任务未认领，
     */
    private Boolean claim;

}
