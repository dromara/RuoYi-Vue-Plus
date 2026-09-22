package org.dromara.workflow.de.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.hr.domain.bo.DeHrAssignBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrHandoverBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrTaskActionBo;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HR 域服务实现。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeHrServiceImpl implements IDeHrService {

    private final FlwUserMapper flwUserMapper;
    private final FlwTaskMapper flwTaskMapper;

    @Override
    public Long assign(DeHrAssignBo bo) {
        if (bo.getTaskId() == null || bo.getInstanceId() == null) {
            throw new ServiceException("taskId / instanceId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null || !task.getInstanceId().equals(bo.getInstanceId())) {
            throw new ServiceException("任务不属于该实例");
        }
        // 主审批人落 type='1'；代理人落 type='3'
        FlowUser approver = ensureRow(bo.getTaskId(), DeP1Constants.USER_TYPE_APPROVER);
        approver.setProcessedBy(bo.getPostId() == null ? approver.getProcessedBy() : String.valueOf(bo.getPostId()));
        flwUserMapper.updateById(approver);

        if (bo.getDelegateId() != null) {
            FlowUser delegate = ensureRow(bo.getTaskId(), DeP1Constants.USER_TYPE_DELEGATE);
            delegate.setProcessedBy(String.valueOf(bo.getDelegateId()));
            flwUserMapper.updateById(delegate);
        }
        log.info("[hr.assign] taskId={} postId={} delegateId={}",
            bo.getTaskId(), bo.getPostId(), bo.getDelegateId());
        return approver.getId();
    }

    @Override
    public Long taskAction(DeHrTaskActionBo bo) {
        if (bo.getAction() == null || bo.getAction().isBlank()) {
            throw new ServiceException("action 必填");
        }
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null || !task.getInstanceId().equals(bo.getInstanceId())) {
            throw new ServiceException("任务不属于该实例");
        }
        // action 路由：仅做最小记录；底座无 action 列，副作用仅更新 message（task 本身无该字段，
        // 故本方法返回 taskId 让调用方知晓命中）
        log.info("[hr.taskAction] taskId={} action={} dueAt={} targetId={}",
            bo.getTaskId(), bo.getAction(), bo.getDueAt(), bo.getTargetId());
        if ("reject".equalsIgnoreCase(bo.getAction())) {
            FlowUser u = ensureRow(bo.getTaskId(), DeP1Constants.USER_TYPE_APPROVER);
            if (u.getId() != null) {
                flwUserMapper.deleteById(u);
            }
        }
        return bo.getTaskId();
    }

    @Override
    public int handover(DeHrHandoverBo bo) {
        if (bo.getFromUserId() == null || bo.getToUserId() == null) {
            throw new ServiceException("fromUserId / toUserId 必填");
        }
        // H1 P1 #22 / #10 零效果负例：fromUserId == toUserId ⇒ 不报错、affected = 0
        // （交接目标是同一人 ⇒ 实际无变化；早返避免无谓写转办 + 改审批人）
        if (bo.getFromUserId().equals(bo.getToUserId())) {
            log.info("[hr.handover] from={} == to={}，零效果早返（无变化）",
                bo.getFromUserId(), bo.getToUserId());
            return 0;
        }
        int affected = 0;
        if (bo.getTaskIds() == null) {
            return 0;
        }
        for (Long taskId : bo.getTaskIds()) {
            FlowUser approver = flwUserMapper.selectOne(
                new LambdaQueryWrapper<FlowUser>()
                    .eq(FlowUser::getAssociated, taskId)
                    .eq(FlowUser::getType, DeP1Constants.USER_TYPE_APPROVER)
            );
            if (approver == null) {
                continue;
            }
            if (String.valueOf(bo.getFromUserId()).equals(approver.getProcessedBy())) {
                // 转办：原审批人保留，转 type='2'
                FlowUser transfer = ensureRow(taskId, DeP1Constants.USER_TYPE_TRANSFER);
                transfer.setProcessedBy(String.valueOf(bo.getToUserId()));
                flwUserMapper.updateById(transfer);

                // 当前审批人改为 toUserId
                approver.setProcessedBy(String.valueOf(bo.getToUserId()));
                flwUserMapper.updateById(approver);
                affected++;
            }
        }
        log.info("[hr.handover] from={} to={} affected={} reason={}",
            bo.getFromUserId(), bo.getToUserId(), affected, bo.getReason());
        return affected;
    }

    private FlowUser ensureRow(Long taskId, String type) {
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