package org.dromara.workflow.de.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.workflow.de.constants.AcquireAgentMode;
import org.dromara.workflow.de.constants.DeBindingConstants;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数字员工绑定请求参数。
 *
 * <p>对应 POST /de/binding 请求体。
 *
 * <p>字段落点（28 号 §一 白名单）：
 * <ul>
 *   <li>instanceId → flow_instance.id（bigint，非自填；由调用方提供已发起的实例 id）</li>
 *   <li>agentId → flow_instance_biz_ext.business_id（varchar(255)，存 sai_agent.id 的字符串形式）</li>
 *   <li>businessTitle → flow_instance_biz_ext.business_title（varchar(1000)）</li>
 * </ul>
 *
 * @author AIP
 */
@Data
public class DeBindingBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance.id（必填） */
    @NotNull(message = "实例 ID 不能为空")
    private Long instanceId;

    /** sai_agent.id（必填；写入 flow_instance_biz_ext.business_id） */
    @NotNull(message = "数字员工 ID 不能为空")
    private Long agentId;

    /**
     * 业务代码（可空；N-03 BO 端白名单校验 —— H1 P1 #16）。
     *
     * <p>仅允许 {@link DeBindingConstants#BINDING_DIGITAL_EMPLOYEE}
     * （即 {@code "digital_employee.binding"}）；传其它值或 null 时按缺省走
     * {@link DeBindingConstants#BINDING_DIGITAL_EMPLOYEE}（向后兼容）。
     */
    private String businessCode;

    /** 业务对象可读标题（必填；写入 flow_instance_biz_ext.business_title） */
    @NotBlank(message = "业务标题不能为空")
    @Size(max = 1000, message = "业务标题长度不能超过 1000 字符")
    private String businessTitle;

    /** sys_user.user_id（必填；N-05 订阅门禁的入参 —— 28 号 §三 AC-07/08 显式提供） */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /**
     * N-05 获取模式（可空；空 = {@code AUTO} = {@code EXCLUSIVE}，向后兼容）。
     *
     * <p>三模式语义见 {@link AcquireAgentMode}：
     * <ul>
     *   <li>{@code SHARED} = 任意订阅即可</li>
     *   <li>{@code EXCLUSIVE} = 必须独占订阅</li>
     *   <li>{@code AUTO}（默认） = 等同 {@code EXCLUSIVE}</li>
     * </ul>
     */
    private AcquireAgentMode acquireAgentMode;
}