package org.dromara.workflow.de.cv.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * CV-02 执行状态查询 VO（记录态 + 锚定实例态双轨）。
 *
 * @author AIP
 */
@Data
public class DeCvStatusVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID */
    private String conversationId;

    /** 会话标题 */
    private String title;

    /** Agent */
    private Long agentId;

    /** 用户 */
    private Long userId;

    /** 锚定流程实例（可为空 = 自由对话） */
    private Long instanceId;

    /** 执行态：queued / running / done / failed / unknown */
    private String executionState;

    /** 最新记录 ID（无记录时为空） */
    private Long recordId;

    /** 最新记录状态（1=成功 2=失败 3=进行中；无记录时为空） */
    private Integer recordStatus;

    /** 最新记录角色 user / assistant */
    private String latestRole;

    /** 最新记录内容预览（≤200 字符） */
    private String latestContentPreview;

    /** 锚定实例的 flow_instance.flow_status（未锚定 / 实例不存在时为空） */
    private String flowStatus;
}
