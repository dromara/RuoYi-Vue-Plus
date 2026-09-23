package org.dromara.workflow.de.cx.service;

import org.dromara.workflow.de.cx.domain.vo.DeCxValidateVo;

/**
 * 上下文构建与消费（CX）服务接口 —— 29 号 §五 CX-10 取 1 端点切片。
 *
 * <p><b>需求原文</b>（{@code docs/设计包/29-全量需求与角色追踪.md} §五 CX 表 CX-10 行）：
 * 「上下文使用和提交时校验当前授权、版本和有效期；验证策略修订号的比较语义」。
 *
 * <p><b>验收</b>：「版本标识不是简单按字典序判断新旧；撤权/过期后不继续读写，
 * 最低版本来自可信服务端事实」。<b>负例</b>：写入上下文文件不能单独证明数字员工已读取。
 *
 * <p><b>落点</b>：{@code flow_instance.variable}（{@code de.ctx}）+ 只读引用
 * {@code sys_user_role.role_id}。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeCxService {

    /**
     * CX-10 上下文使用 / 提交校验。
     *
     * @param instanceId 目标实例 ID（flow_instance.id）
     * @param action     校验时点：read（使用）/ write（提交）；空 = read
     * @param userId     当前登录用户 ID（由调用方从会话取，便于单测直调）
     * @param username   当前登录用户名（实例发起人分支用；可空）
     * @return 校验结果（valid / reason / currentVersion / minVersion 等）
     */
    DeCxValidateVo contextValidate(Long instanceId, String action, Long userId, String username);

    /**
     * 服务端最低版本（可信服务端事实）。
     *
     * <p>只从服务端常量取，<b>不接受</b>任何调用方传入值 —— CX-10 验收原句
     * 「最低版本来自可信服务端事实」。
     */
    int serverMinVersion();
}
