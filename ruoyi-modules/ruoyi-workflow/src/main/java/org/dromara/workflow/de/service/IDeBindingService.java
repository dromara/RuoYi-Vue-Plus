package org.dromara.workflow.de.service;

import org.dromara.workflow.de.domain.bo.DeAssigneeBo;
import org.dromara.workflow.de.domain.bo.DeCandidatesBo;
import org.dromara.workflow.de.domain.bo.DeVariableBo;
import org.dromara.workflow.de.domain.bo.DeBindingBo;
import org.dromara.workflow.de.domain.vo.DeBindingVo;
import org.dromara.workflow.de.domain.vo.DeVariableAckVo;

/**
 * 数字员工绑定服务接口。
 *
 * <p>P0：createBinding / getBinding（接缝定桩）—— 28 号 §三 AC-01 / AC-02 / AC-03
 * <p>P1：updateVariable / updateCandidates / assignUser（运行态与待办责任人）—— 28 号 §三 AC-04 / AC-05 / AC-06
 * <p><b>AU-09 历史对话摘要派生（D-002 已登记，已实装）</b>：{@link #getBinding(Long)}
 * 在 JOIN sai_agent 取 {@code agent_name} 后，再读 sai_agent_conversation 表
 * 按 {@code create_dt DESC LIMIT 1} 取最近一条的 {@code title} 回填到
 * {@code DeBindingVo.title}。<b>只读派生</b>，不写 sai_agent_conversation 表。
 * 真源 = {@code 04-backend-code/script/sql/ry_ai.sql} 7 列 DDL（与
 * {@code docs/底座真实表结构.md:263-273} 一致）。
 *
 * @author AIP
 */
public interface IDeBindingService {

    // ===== P0 方法（不变）=====
    /**
     * 写绑定（D7 B 路径：应用层先 SELECT 再 INSERT）。
     *
     * @param bo 请求参数
     * @return 写入的主键 id（flow_instance_biz_ext.id）
     * @throws org.dromara.common.core.exception.ServiceException 若已存在同 (instanceId, businessCode) 的绑定
     */
    Long createBinding(DeBindingBo bo);

    /**
     * 读绑定：JOIN sai_agent 取 agent_name + <b>AU-09 派生</b>读 sai_agent_conversation
     * 取最近一条 title（按 {@code create_dt DESC LIMIT 1}）。两者均<b>只读</b>。
     *
     * @param instanceId flow_instance.id
     * @return 单条绑定 VO；未绑定返回 null（AC-03 负例）。{@code title} 字段在该实例无历史
     *         conversation 时为 {@code null}（AC-09 边界）。
     */
    DeBindingVo getBinding(Long instanceId);

    /**
     * D-010 补缺：绑定分页列表（GET /de/binding/list，前端 List.vue 的数据源）。
     * 富化逻辑与 getBinding 同源（agentName / title 派生），但空行不抛错（列表语义）。
     */
    org.dromara.common.core.domain.PageResult<DeBindingVo> listBindings(
            Long instanceId, String businessCode, Long agentId,
            org.dromara.common.mybatis.core.page.PageQuery pageQuery);

    // ===== P1 新增方法 =====

    /**
     * 写实例运行态（AC-04）。含 N-04 应用层形状校验（de.ctx 顶层 + 4 必填键 + task.step 枚举）。
     *
     * @param bo 请求参数（value = de.ctx JSON 字符串）
     * @return 写入响应（validated=true 表示校验通过并落库）
     * @throws org.dromara.common.core.exception.ServiceException 校验失败时拒收
     */
    DeVariableAckVo updateVariable(DeVariableBo bo);

    /**
     * 写数字员工候选集（AC-05）。纯 int 数组；超 500 字符拒收。
     *
     * @param bo 请求参数
     * @return true 写入成功
     * @throws org.dromara.common.core.exception.ServiceException 超 500 字符时拒收
     */
    Boolean updateCandidates(DeCandidatesBo bo);

    /**
     * 写待办责任人（AC-06）。落点：flow_user(type='1', processed_by, associated=taskId)。
     *
     * @param bo 请求参数
     * @return flow_user.id（新建 or 已存在行的 id）
     */
    Long assignUser(DeAssigneeBo bo);

    // ===== AU-12 应用层解（27 号 §7.7 / D-002 已登记）=====

    /**
     * 按实例 ID 删除绑定行（A2 §三 AU-12 「应用可解」窄边界）。
     *
     * <p>底座 <code>flwInstanceBizExtMapper.deleteByInstId(Long)</code>
     * （{@code FlwInstanceBizExtMapper.java:45}）<b>定义存在、调用点为 0</b>（27 号 §7.7 已登记）。
     * 本方法 <b>不补底座 controller 调用点</b>（那是 D-00X 范畴，须 owner 裁）——
     * 仅在 de/ 层暴露服务方法 + 一个 DELETE 端点，让 P0 接口侧 <b>可调用</b>。
     *
     * <p>本方法不会因为底座 controller 不调它就「永不被触发」——
     * 一旦 owner 裁 D-00X 在底座删除路径补本调用，<b>删除实例 → 联动清空绑定行</b>即生效。
     *
     * @param instanceId flow_instance.id
     * @return 实际删除行数（0 = 该实例无绑定，符合 N-07 / AC-03 判别力）
     */
    int deleteBindingByInstanceId(Long instanceId);
}