package org.dromara.workflow.de.cv.service;

import org.dromara.workflow.de.cv.domain.bo.DeCvExecuteBo;
import org.dromara.workflow.de.cv.domain.vo.DeCvExecuteResultVo;
import org.dromara.workflow.de.cv.domain.vo.DeCvSessionVo;
import org.dromara.workflow.de.cv.domain.vo.DeCvStatusVo;

import java.util.List;

/**
 * 对话与执行（CV）服务接口 —— 29 号 §五 CV-01~CV-07 取 3 端点切片。
 *
 * <p>落点真源：sai_agent_conversation（会话列表；instance_id 锚定流程实例）
 * ＋ sai_agent_conversation_record.status（执行状态：1=成功 2=失败 3=进行中）
 * ＋ 只读引用 flow_instance.flow_status（CV-02 双轨口径）。
 *
 * <p>与 de-conversation 模块的 create / message / complete 并列不重复：
 * 本域补会话列表、执行触发与状态查询。
 *
 * @author AIP
 */
public interface IDeCvService {

    /**
     * 会话列表：按用户（必填）/ Agent（可选）列出 sai_agent_conversation 行。
     */
    List<DeCvSessionVo> listSessions(Long userId, Long agentId);

    /**
     * CV-01 执行触发：校验会话存在后落一条 user 记录（status=3 进行中），
     * 返回执行句柄（recordId + 会话锚定信息）。
     */
    DeCvExecuteResultVo execute(DeCvExecuteBo bo);

    /**
     * CV-02 执行状态查询：会话最新记录 status 映射执行态
     * （无记录=queued / 3=running / 1=done / 2=failed），
     * 锚定实例时并回 flow_instance.flow_status。
     */
    DeCvStatusVo status(String conversationId);
}
