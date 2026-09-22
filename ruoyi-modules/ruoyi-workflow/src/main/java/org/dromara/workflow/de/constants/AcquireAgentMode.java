package org.dromara.workflow.de.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 数字员工绑定时的「获取模式」枚举（N-05 升级用）。
 *
 * <p>三种模式的语义对比：
 * <table>
 *   <caption>三模式行为对照</caption>
 *   <tr><th>模式</th><th>订阅检查</th><th>是否要求独占</th><th>向后兼容</th></tr>
 *   <tr><td>{@link #SHARED}</td><td>用户必须有订阅</td><td>否（其他用户可同时订阅）</td><td>否（新模式）</td></tr>
 *   <tr><td>{@link #EXCLUSIVE}</td><td>用户必须有订阅</td><td><b>是</b>（无其他用户订阅该 agent）</td><td>否（新模式）</td></tr>
 *   <tr><td>{@link #AUTO}</td><td>等同 {@link #EXCLUSIVE}</td><td>是（默认独占）</td><td><b>是</b>（P0/P1/P2 已 done 行为）</td></tr>
 * </table>
 *
 * <p><b>{@code AUTO} = 默认 {@code EXCLUSIVE}</b>，保留历史 P0/P1/P2 已验收通过的行
 * 为不变 —— 旧调用方不传该字段时，{@link DeBindingBo#getAcquireAgentMode()} 为
 * {@code null}，{@link #effective(AcquireAgentMode)} 将其解析为 {@link #AUTO}，
 * {@link #AUTO} 再解析为 {@link #EXCLUSIVE}。
 *
 * <p>序列化值（{@link JsonValue} / MyBatis-Plus {@link EnumValue}）= 字符串字面，
 * 大写。HTTP 接口 JSON 示例：{@code "acquireAgentMode": "SHARED"}。
 *
 * @author AIP
 */
public enum AcquireAgentMode {

    /** 任意订阅即可：只要用户在该 agent 上有一行订阅就允许绑定（与"独占"无关）。 */
    SHARED,

    /** 必须独占订阅：用户必须有订阅，且该 agent 上无其他用户的订阅。 */
    EXCLUSIVE,

    /** 自动模式：默认 {@link #EXCLUSIVE}。保留历史行为，向后兼容。 */
    AUTO;

    /**
     * 将传入模式（含 {@code null}）解析为有效模式。
     *
     * <p>{@code null} 或 {@link #AUTO} ⇒ 返回 {@link #EXCLUSIVE}。
     *
     * @param mode 原始模式（可为 {@code null}）
     * @return 解析后的有效模式（不会返回 {@code null} 或 {@link #AUTO}）
     */
    public static AcquireAgentMode effective(AcquireAgentMode mode) {
        if (mode == null || mode == AUTO) {
            return EXCLUSIVE;
        }
        return mode;
    }
}
