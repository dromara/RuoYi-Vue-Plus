package org.dromara.workflow.de.service;

import java.util.Map;

/**
 * P1 档 B + 档 D 共 41 项需求的服务接口（声明式占位）。
 *
 * <p>本接口对应 {@code docs/P1需求实施清单-2026-09-18.md} §三（档 B 16 项）+ §五（档 D 25 项）= 41 项。
 *
 * <p><b>约束</b>：
 * <ol>
 *   <li><b>档 B（16 项）</b> = 需新切片 P5 / P6 —— 方法体仅返回占位响应 + 日志标注「档 B 待 owner 裁」。</li>
 *   <li><b>档 D（25 项）</b> = 仓外依赖（{@code sai_*} 端点 / IM 渠道服务 / L3 上下文装配层）——
 *       方法体仅返回占位响应 + 日志标注「owner 待裁」，待仓外服务接口清单锁定后再真接。</li>
 *   <li>不发明新表 / 不发明列。</li>
 *   <li>不动底座正文。</li>
 * </ol>
 *
 * <p>每个方法的响应字段固定包含：{@code status}（DEFERRED_TO_OWNER_DECISION）、
 * {@code gapCategory}（B / D）、{@code requirementId}（如 SK-03）、{@code landingPoint}
 * （真实落点表.列引用）、{@code note}（一句话说明）。
 *
 * @author AIP
 */
public interface IDeP1GapCoverService {

    // ===== 档 B（16 项 · 需新切片 P5 / P6）=====

    /** SK-03 节点填写技能业务参数 + 多技能排序 / 依赖 / 冲突提示（G-11 残余） */
    Map<String, Object> bSk03ConfigCatalog(Object payload);

    /** CN-07 完善浏览器 / 电脑 / 文件 / 命令能力的节点入口（G-09） */
    Map<String, Object> bCn07ToolDiscovery(Object payload);

    /** KB-06 节点区分流程必需知识与员工追加知识 + 显式展示冲突 / 过期（G-10） */
    Map<String, Object> bKb06RequiredVsAdded(Object payload);

    /** WS-03 确需本机资源的任务提供受控设备 / 浏览器桥接（G-09） */
    Map<String, Object> bWs03LocalBridge(Object payload);

    /** WS-05 节点产物列表 / 差异 / 下载 / 版本关系（G-20） */
    Map<String, Object> bWs05ProductVersion(Object payload);

    /** SY-04 结构化事实与文件协作分别处理并发冲突 + 保留候选和来源（G-16） */
    Map<String, Object> bSy04FactConflict(Object payload);

    /** OP-03 员工进度 / 成本 / 可恢复错误 + 运维运行健康 / 技术诊断（G-14） */
    Map<String, Object> bOp03PersonalHealth(Object payload);

    /** OP-04 区分模型 / 工具消耗 / 运行成功率 / 业务完成率 / 返工 / 交接等待（G-14） */
    Map<String, Object> bOp04MetricsBreakdown(Object payload);

    /** OP-05 提供语言 / 回复偏好 / 时区 / 通知 / 会话 / 可访问性设置（G-14） */
    Map<String, Object> bOp05Preferences(Object payload);

    /** OP-06 管理执行环境 / 设备连接 / 模型健康 / 容量 / 断网 / 重启恢复（G-14） */
    Map<String, Object> bOp06RuntimeEnv(Object payload);

    /** MM-01 明确个人 / Agent / 项目 / 团队 / 节点实例 / 渠道记忆的适用范围（G-11） */
    Map<String, Object> bMm01Scope(Object payload);

    /** MM-02 提供分类 / 搜索 / 来源 / 时间线 / 编辑 / 失效 / 导出 / 保留设置（G-11） */
    Map<String, Object> bMm02Organization(Object payload);

    /** AT-02 周期 / 一次性 / 事件 / API / 条件拉取统一配置输入和执行对象（G-02 残余） */
    Map<String, Object> bAt02TriggerTypes(Object payload);

    /** AT-03 展示时区 / 下一次时间 / 最大次数 / 截止时间 / 暂停 / 恢复（G-02 残余） */
    Map<String, Object> bAt03Timezone(Object payload);

    /** AT-05 自动化随节点完成 / 权限变化 / 资源失效 / 人员离岗联动（G-02 残余） */
    Map<String, Object> bAt05Linkage(Object payload);

    /** IM-01 将渠道连接 / 聊天接入 / 成员身份绑定做成明确向导（G-18） */
    Map<String, Object> bIm01ChannelBinding(Object payload);

    // ===== 档 D（25 项 · 仓外依赖）=====

    /** AG-03 数字员工创建 / 编辑 / 草稿（落 sai_agent.name + sai_agent.instruction） */
    Map<String, Object> dAg03AgentEdit(Object payload);

    /** SK-04 技能上传 / 解析 / 隔离试验 / 提交发布（落 sai_skill.skill_content + sai_skill_file.file_path） */
    Map<String, Object> dSk04UploadPublish(Object payload);

    /** SK-05 节点内试用技能（落 sai_skill.id + sai_agent_conversation_record.content） */
    Map<String, Object> dSk05NodeTrial(Object payload);

    /** CN-06 自定义 MCP / HTTP 连接器向导 + 工具发现 + 只读连通性验证（落 sai_mcp_server.transport_type + command + base_uri） */
    Map<String, Object> dCn06CustomMcp(Object payload);

    /** CN-08 写操作预览 / 确认 / 超时查证 / 重复调用保护（落 sai_agent_conversation_record.metadata + status） */
    Map<String, Object> dCn08WritePreview(Object payload);

    /** KB-02 文件 / URL / 业务系统来源导入 + 解析 / 加工 / 发布（落 sai_rag_document.status + source_type + source_path） */
    Map<String, Object> dKb02DocImport(Object payload);

    /** KB-03 主题 / 文件夹 / 模板化加工 / 定期更新（落 sai_rag_document.doc_metadata + content_hash） */
    Map<String, Object> dKb03DocProcess(Object payload);

    /** KB-04 关键词 / 语义 / 混合检索 + 评测集（落 sai_rag.search_engine_enable + rag_enhancement） */
    Map<String, Object> dKb04SearchMode(Object payload);

    /** KB-05 在节点对话 / 产物中提供可打开引用 / 版本 / 片段（落 sai_rag_chunk.content + document_id） */
    Map<String, Object> dKb05ChunkCitation(Object payload);

    /** MM-03 展示「本次使用了哪些记忆」+ 纠正 / 忽略 / 冲突（落 sai_agent_conversation_record.metadata + flow_instance.variable） */
    Map<String, Object> dMm03UsedList(Object payload);

    /** MM-04 任务复盘经验以建议方式呈现（落 sai_agent_conversation_record.content） */
    Map<String, Object> dMm04ReviewTip(Object payload);

    /** CV-03 补充信息 / 暂停 / 停止 / 继续 / 人工接管（落 flow_task.flow_status + sai_agent_conversation_record.status，AU-08） */
    Map<String, Object> dCv03PauseResume(Object payload);

    /** CV-04 同一任务多轮修改 / 引用产物 / 历史（落 flow_instance.variable + sai_agent_conversation_record.content，AU-08） */
    Map<String, Object> dCv04MultiTurn(Object payload);

    /** CV-05 工具调用 / 待确认问题转业务可读卡片（落 flow_instance.variable + sai_agent_conversation_record.metadata，AU-08） */
    Map<String, Object> dCv05ToolCallCard(Object payload);

    /** CV-06 自由对话转有明确责任任务（落 flow_task.id + flow_instance.id，AU-08） */
    Map<String, Object> dCv06ChatToTask(Object payload);

    /** CV-07 运行失败保留产物 / 已发生动作 / 可恢复下一步（落 flow_instance.variable + sai_agent_conversation_record.content，AU-08） */
    Map<String, Object> dCv07FailureRecover(Object payload);

    /** CX-02 任务流水统一收录 + 按条目授权（落 flow_instance.variable + sai_agent_conversation_record.content，AU-08） */
    Map<String, Object> dCx02StreamArchive(Object payload);

    /** CX-04 必需规则固定保留 + 较长历史按摘要 / 检索装配（落 flow_instance.variable + sai_rag_chunk.content，AU-08） */
    Map<String, Object> dCx04RuleKeep(Object payload);

    /** CX-07 划分共享 / 私人 / 私有记忆 / 连接身份（落 flow_instance.variable + sai_agent_conversation_record.user_id，AU-08） */
    Map<String, Object> dCx07MemoryScope(Object payload);

    /** CX-08 上下文引用附件 / 知识 / 业务记录 / 工具结果 时固定可解析版本（落 flow_instance.variable + sai_rag_document.content_hash，AU-08） */
    Map<String, Object> dCx08ContextVersion(Object payload);

    /** CX-09 关键任务公开目标 / 约束理解摘要 + 判定完成检查（落 flow_instance.variable + flow_his_task.message，AU-08） */
    Map<String, Object> dCx09GoalSummary(Object payload);

    /** CX-12 UI 分别展示上下文准备 / 投递 / 装载 / 更新待应用 / 失效 / 冲突（落 flow_instance.variable + flow_task.flow_status，AU-08） */
    Map<String, Object> dCx12ContextStatus(Object payload);

    /** HC-07 为人—Agent / Agent—Agent 协作请求标记目标 / 用途 / 接收者 / 允许动作（落 flow_instance.variable + sai_agent_conversation.agent_id） */
    Map<String, Object> dHc07CollabMeta(Object payload);

    /** IM-02 从群 / 单聊发起 / 跟进节点任务 / 返回进度（落 flow_task.id + flow_instance.id） */
    Map<String, Object> dIm02TaskProgress(Object payload);

    /** IM-03 配置专业 Agent / 默认 Agent / 职责冲突提示 / 可审查路由（落 sai_agent.id + sai_agent_conversation.agent_id） */
    Map<String, Object> dIm03AgentRouting(Object payload);
}