package org.dromara.workflow.de.cx.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * CX-10 上下文校验结果 VO。
 *
 * <p>{@code valid=false} 时 {@code reason} 给出唯一拒因（fail-fast，不叠加多条）——
 * 调用方按 {@code reason} 分支处理，不解析自然语言。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeCxValidateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否放行本次使用 / 提交 */
    private Boolean valid;

    /** 唯一判因（见 {@code DeCxServiceImpl} 的 REASON_* 常量） */
    private String reason;

    /** 上下文自身版本（flow_instance.variable 的 de.ctx.v；缺 / 不可解析 = null） */
    private Integer currentVersion;

    /** 服务端最低版本（服务端常量，非请求体传入） */
    private Integer minVersion;

    /** 被校验的实例 ID */
    private Long instanceId;

    /** 归一化后的校验时点（read / write） */
    private String action;

    /** 当前用户在 sys_user_role 里的角色 ID 列表（实时读库；空 = 无角色） */
    private List<Long> roleIds;

    /** 上下文有效期（de.ctx.expires_at；上下文未声明 = null） */
    private Date expiresAt;

    /** 判定时刻（服务端时钟） */
    private Date checkedAt;
}
