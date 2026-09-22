package org.dromara.workflow.de.constants;

/**
 * 数字员工绑定服务的应用层错误码枚举。
 *
 * <p>本类是 N-04 / N-05 / N-06 应用层校验的<b>错误码单一真源</b>。
 * 每个失败字段 / 失败模式都有<b>独立的错误码</b>，便于调用方按错误码分支处理、
 * 上报渠道做归因聚合、监控指标按错误码切片。
 *
 * <p>码段划分（避免与底座全局错误码冲突，本仓独占 1xxxx-3xxxx 段）：
 * <ul>
 *   <li>1xxxx = N-04 形状校验（{@code flow_instance.variable} 的 {@code de.ctx}）</li>
 *   <li>2xxxx = N-05 订阅门禁</li>
 *   <li>3xxxx = N-06 知识库个数门禁 + 实例存在性 + 重复绑定</li>
 * </ul>
 *
 * <p><b>错误码一旦发布不可回收</b> —— 调整语义须新增错误码并保留旧码作 deprecated。
 *
 * @author AIP
 */
public enum DeErrorCode {

    // ===== N-04 形状校验（1xxxx）=====

    /** variable 字符串不是合法 JSON（解析阶段即失败） */
    DE_VAR_INVALID_JSON(1001, "variable 不是合法 JSON: {0}"),
    /** 缺少顶层键 {@code de.ctx}（违反 27 号 §5.2 硬约束 1） */
    DE_VAR_MISSING_TOP_KEY(1002, "variable 缺少必填顶层键 'de.ctx'"),
    /** {@code de.ctx.v} 缺失或不是 schema 版本号 1 */
    DE_VAR_INVALID_VERSION(1003, "de.ctx.v 缺失或不是 {0}"),
    /** {@code de.ctx.binding.agent_id} 缺失 */
    DE_VAR_MISSING_BINDING_AGENT_ID(1004, "de.ctx.binding.agent_id 必填"),
    /** {@code de.ctx.binding.business_code} 缺失 */
    DE_VAR_MISSING_BINDING_BUSINESS_CODE(1005, "de.ctx.binding.business_code 必填"),
    /** {@code de.ctx.task.step} 缺失 */
    DE_VAR_MISSING_TASK_STEP(1006, "de.ctx.task.step 必填"),
    /** {@code de.ctx.task.step} 不在四值枚举内 */
    DE_VAR_INVALID_TASK_STEP(1007, "de.ctx.task.step 枚举越界: {0}"),
    /** {@code de.ctx.binding.business_code} 不是白名单值（仅允许 'digital_employee.binding'） */
    DE_VAR_INVALID_BINDING_BUSINESS_CODE(1008, "de.ctx.binding.business_code 非白名单值: {0}（白名单={1}）"),
    /** {@code de.ctx.binding.agent_id} 不是正整数（必须 ≥ 1 的整数） */
    DE_VAR_INVALID_BINDING_AGENT_ID(1009, "de.ctx.binding.agent_id 非法: {0}（必须是 ≥ 1 的整数）"),
    /** {@code de.ctx.tokens.{input|output|cache}} 类型错或 < 0（H1 P1 #7 / M#5 / AC-11） */
    DE_VAR_INVALID_TOKENS_FIELD(1010, "de.ctx.tokens.{0} 非法: {1}（必须是 ≥ 0 的整数）"),
    /** flow_instance.variable 字符串超 65536 字符上限（H1 P1 #20 / M#4 / 27 号 §5.2） */
    DE_VAR_SIZE_EXCEEDED(1011, "variable 长度 {0} 超过上限 {1}（27 号 §5.2 硬约束 2）"),

    // ===== N-05 订阅门禁（2xxxx）=====

    /** 用户对该 agent 无订阅记录（shared / auto 模式均拒） */
    DE_N05_NOT_SUBSCRIBED(2001, "用户 {0} 未订阅数字员工 {1}，订阅门禁拒绝（N-05 / AC-08）"),
    /** 用户对该 agent 的订阅不是独占（exclusive / auto 模式拒；shared 通过） */
    DE_N05_NOT_EXCLUSIVE(2002, "用户 {0} 对数字员工 {1} 的订阅不是独占，exclusive 模式拒绝（N-05）"),

    // ===== N-06 / 通用（3xxxx）=====

    /** 数字员工不存在（createBinding 前置检查） */
    DE_AGENT_NOT_FOUND(3001, "数字员工不存在: id={0}"),
    /** sai_agent.rag_ids 拆出 ID 个数超上限 */
    DE_RAG_COUNT_EXCEEDED(3002, "数字员工 {0} 的 rag_ids 个数为 {1}，超过上限 {2}（N-06）"),
    /** flow_instance 不存在 */
    DE_INSTANCE_NOT_FOUND(3003, "实例不存在: id={0}"),
    /** 同一实例已存在绑定（D7 B 路径兜底） */
    DE_BINDING_DUPLICATE(3004, "实例 {0} 已存在数字员工绑定，请勿重复绑定"),
    /** DeBindingBo.businessCode 非白名单（H1 P1 #16 / N-03 BO 端） */
    DE_BINDING_INVALID_BUSINESS_CODE(3009, "DeBindingBo.businessCode 非白名单值: {0}（白名单={1}）"),
    /** flow_node.ext 解析失败（WS 域读路径 H1 P1 #3） */
    DE_NODE_EXT_INVALID_JSON(3005, "flow_node.ext 解析失败: {0}"),
    /** flow_node.ext 序列化失败（WS 域写路径 H1 P1 #2） */
    DE_NODE_EXT_SERIALIZE_FAILED(3006, "flow_node.ext 序列化失败: {0}"),
    /** N-05 sai_user_agent 计数失败（H1 P1 #4 —— 静默返 0 会掩盖真实 mapper 异常） */
    DE_N05_COUNT_FAILED(3007, "sai_user_agent 计数失败: {0}"),
    /** N-07 责任人不属于 sai_user_agent 订阅（H1 P1 #12 / 28 号 §四 4.2） */
    DE_N07_NOT_IN_USER_AGENT(3008, "责任人 userId={0} 不在 sai_user_agent 订阅（agentId={1}），N-07 门禁拒绝");

    private final int code;
    private final String messageTemplate;

    DeErrorCode(int code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public int getCode() {
        return code;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * 用 {@link java.text.MessageFormat} 风格占位符格式化错误消息。
     *
     * @param args 占位符参数
     * @return 格式化后的错误消息
     */
    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        return java.text.MessageFormat.format(messageTemplate, args);
    }

    @Override
    public String toString() {
        return "DE-" + code;
    }
}
