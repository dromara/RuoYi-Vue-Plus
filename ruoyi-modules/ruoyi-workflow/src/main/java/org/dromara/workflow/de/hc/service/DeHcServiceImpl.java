package org.dromara.workflow.de.hc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.hc.domain.bo.DeHcCollabRequestBo;
import org.dromara.workflow.de.hc.domain.bo.DeHcParticipantBo;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * HC 域服务实现。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeHcServiceImpl implements IDeHcService {

    private final FlwTaskMapper flwTaskMapper;
    private final FlwUserMapper flwUserMapper;

    @Override
    public Long collabRequest(DeHcCollabRequestBo bo) {
        if (bo.getFromUserId() == null || bo.getToUserId() == null) {
            throw new ServiceException("fromUserId / toUserId 必填");
        }
        log.info("[hc.collab] from={} to={} target={}", bo.getFromUserId(), bo.getToUserId(), bo.getTarget());
        return System.currentTimeMillis(); // 占位 ID
    }

    @Override
    public Long participant(DeHcParticipantBo bo) {
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + bo.getTaskId());
        }
        if ("leave".equalsIgnoreCase(bo.getAction())) {
            FlowUser u = flwUserMapper.selectOne(
                new LambdaQueryWrapper<FlowUser>()
                    .eq(FlowUser::getAssociated, task.getId())
                    .eq(FlowUser::getType, DeP1Constants.USER_TYPE_APPROVER)
            );
            if (u != null && String.valueOf(bo.getUserId()).equals(u.getProcessedBy())) {
                flwUserMapper.deleteById(u);
            }
        } else if ("join".equalsIgnoreCase(bo.getAction())) {
            FlowUser row = ensure(task.getId(), DeP1Constants.USER_TYPE_APPROVER);
            row.setProcessedBy(String.valueOf(bo.getUserId()));
            flwUserMapper.updateById(row);
        }
        log.info("[hc.participant] taskId={} action={} userId={}", bo.getTaskId(), bo.getAction(), bo.getUserId());
        return bo.getTaskId();
    }

    @Override
    public Map<String, Object> contributionStatus(Long collabId) {
        if (collabId == null) {
            throw new ServiceException("collabId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("collabId", collabId);
        out.put("status", "draft");
        out.put("typeHistory", new String[]{"draft", "submitted", "accepted", "rejected", "replaced"});
        return out;
    }

    @Override
    public Map<String, Object> nodeParticipants(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("taskId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + taskId);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("taskId", taskId);
        out.put("participants", new ArrayList<>());
        out.put("agentAuthorizations", new ArrayList<>());
        return out;
    }

    private FlowUser ensure(Long taskId, String type) {
        FlowUser existing = flwUserMapper.selectOne(
            new LambdaQueryWrapper<FlowUser>()
                .eq(FlowUser::getAssociated, taskId)
                .eq(FlowUser::getType, type)
        );
        FlowUser row = existing == null ? new FlowUser() : existing;
        row.setAssociated(taskId);
        row.setType(type);
        return row;
    }
}