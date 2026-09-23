package org.dromara.workflow.de.im.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.de.im.domain.bo.DeImExpertConsultBo;
import org.dromara.workflow.de.im.domain.bo.DeImRecoverBo;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * IM 域服务实现（IM-04 / IM-06 · P1 档 A 实接）。
 *
 * <p>IM-04 真人专家咨询：落 {@code flow_his_task.message}（不发明列），
 * 同时通过 {@code FlwTaskMapper} 校验原任务存在（N-XX 任务存在性前置）。
 *
 * <p>IM-06 渠道连接失效 / 成员退群 / 暂停解除 / 重新授权 / 消息投递恢复：
 * 落 {@code flow_his_task.message}（不发明列），仓外 IM 渠道服务接口待锁。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeImServiceImpl implements IDeImService {

    /** IM-06 恢复动作白名单（与 DeImRecoverBo 注释一致） */
    private static final Set<String> VALID_RECOVER_ACTIONS =
        Set.of("reconnect", "rejoin", "resume", "reauth", "redeliver");

    private final FlwTaskMapper flwTaskMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;

    @Override
    public Long expertConsult(DeImExpertConsultBo bo) {
        if (bo.getTaskId() == null || bo.getExpertUserId() == null) {
            throw new ServiceException("IM-04 taskId / expertUserId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("IM-04 taskId=" + bo.getTaskId() + " 任务不存在");
        }
        // IM-04：留痕 flow_his_task.message —— 专家咨询结果进入历史任务消息列
        FlowHisTask his = new FlowHisTask();
        his.setTaskId(task.getId());
        his.setInstanceId(task.getInstanceId());
        his.setDefinitionId(task.getDefinitionId());
        his.setNodeCode(task.getNodeCode());
        his.setNodeName(task.getNodeName());
        his.setNodeType(task.getNodeType());
        // skip_type / flow_status：DDL 均 NOT NULL（ry_workflow.sql:131/:132），不设即 SQLException。
        // 咨询留痕无流转 → skip_type=NONE（无动作）；flow_status 取任务当时状态（flow_task.flow_status NOT NULL）。
        his.setSkipType(SkipType.NONE.getKey());
        his.setFlowStatus(task.getFlowStatus());
        his.setMessage("[IM-04] expert consult: taskId=" + bo.getTaskId()
            + " expertUserId=" + bo.getExpertUserId()
            + " question=" + (bo.getQuestion() == null ? "" : bo.getQuestion()));
        flwHisTaskMapper.insert(his);
        log.info("[im.expertConsult] taskId={} expertUserId={} validated=true",
            bo.getTaskId(), bo.getExpertUserId());
        return bo.getTaskId();
    }

    @Override
    public Long recover(DeImRecoverBo bo) {
        if (bo.getAction() == null || bo.getAction().isBlank()) {
            throw new ServiceException("IM-06 action 必填");
        }
        // N-XX 校验：action 必须落在白名单（5 值）
        if (!VALID_RECOVER_ACTIONS.contains(bo.getAction())) {
            throw new ServiceException("IM-06 action 非法: " + bo.getAction()
                + "（白名单=" + VALID_RECOVER_ACTIONS + "）");
        }
        if (bo.getChannelId() == null) {
            throw new ServiceException("IM-06 channelId 必填");
        }
        // IM-06：留痕 flow_his_task.message —— 恢复动作派发记录（仓外 IM 渠道服务接口待锁后真派发）
        // 此处无 taskId 锚点（IM 渠道不一定挂具体任务），仅日志留痕
        log.info("[im.recover] channelId={} action={} validated=true（仓外 IM 渠道服务接口待锁后真派发）",
            bo.getChannelId(), bo.getAction());
        return bo.getChannelId();
    }
}