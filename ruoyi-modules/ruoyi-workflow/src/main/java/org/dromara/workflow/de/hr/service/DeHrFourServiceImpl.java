package org.dromara.workflow.de.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.hr.domain.bo.DeHrThreeTierBo;
import org.dromara.workflow.de.hr.domain.vo.DeHrThreeTierVo;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * HR-04 三分位落点服务实现（2026-09-23）。
 *
 * <p><b>复用既有表</b>：flow_user(type=1/2/3 + processed_by + associated=taskId)
 * + flow_instance.variable JSON 分段。本类不新造表（CLAUDE.md §「最要紧的一条约束」）。
 *
 * <p><b>三分位 service 串行</b>：approver（type=1）→ transferTo（type=2）→
 * delegateTo（type=3）依序写入 flow_user，每个 ensureRow 调用同 mapper。
 * 三分位 JSON 同时写入 {@code flow_instance.variable.de.ctx.hr.three_tier}。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeHrFourServiceImpl implements IDeHrFourService {

    private final FlwUserMapper flwUserMapper;
    private final FlwInstanceMapper flwInstanceMapper;
    private final ObjectMapper objectMapper;

    private static final String THREE_TIER_KEY = "de.ctx.hr.three_tier";

    @Override
    public DeHrThreeTierVo getThreeTier(Long instanceId, Long taskId) {
        if (instanceId == null) {
            throw new ServiceException("instanceId 必填");
        }
        FlowInstance inst = flwInstanceMapper.selectById(instanceId);
        if (inst == null) {
            throw new ServiceException("实例不存在");
        }
        DeHrThreeTierVo vo = new DeHrThreeTierVo();
        vo.setInstanceId(instanceId);
        vo.setTaskId(taskId);
        // 1) 读 variable 三分位 JSON 段
        String threeTierJson = readThreeTierFromVariable(inst.getVariable());
        vo.setThreeTierJson(threeTierJson);
        // 2) 若指定 taskId，读 flow_user 三行（type=1/2/3）
        if (taskId != null) {
            FlowUser approver = findUser(taskId, DeP1Constants.USER_TYPE_APPROVER);
            FlowUser transfer = findUser(taskId, DeP1Constants.USER_TYPE_TRANSFER);
            FlowUser delegate = findUser(taskId, DeP1Constants.USER_TYPE_DELEGATE);
            vo.setApprover(approver == null ? null : approver.getProcessedBy());
            vo.setTransferTo(transfer == null ? null : transfer.getProcessedBy());
            vo.setDelegateTo(delegate == null ? null : delegate.getProcessedBy());
        }
        return vo;
    }

    @Override
    public DeHrThreeTierVo upsertThreeTier(DeHrThreeTierBo bo) {
        if (bo.getInstanceId() == null) {
            throw new ServiceException("instanceId 必填");
        }
        FlowInstance inst = flwInstanceMapper.selectById(bo.getInstanceId());
        if (inst == null) {
            throw new ServiceException("实例不存在");
        }
        // 1) 写 flow_user 三行（type=1/2/3）—— 仅当 taskId 给出
        if (bo.getTaskId() != null) {
            upsertUser(bo.getTaskId(), DeP1Constants.USER_TYPE_APPROVER, bo.getApprover());
            upsertUser(bo.getTaskId(), DeP1Constants.USER_TYPE_TRANSFER, bo.getTransferTo());
            upsertUser(bo.getTaskId(), DeP1Constants.USER_TYPE_DELEGATE, bo.getDelegateTo());
        }
        // 2) 写 variable.de.ctx.hr.three_tier JSON
        String variable = inst.getVariable();
        String json = bo.getThreeTierJson();
        if (json != null && !json.isBlank()) {
            try {
                objectMapper.readTree(json); // 校验合法性
            } catch (Exception e) {
                throw new ServiceException("threeTierJson 非合法 JSON");
            }
            String updated = writeThreeTierToVariable(variable, json);
            inst.setVariable(updated);
            inst.setUpdateTime(new Date());
            flwInstanceMapper.updateById(inst);
        }
        log.info("[hr.threeTier] instanceId={} taskId={} approver={} transfer={} delegate={}",
            bo.getInstanceId(), bo.getTaskId(), bo.getApprover(), bo.getTransferTo(), bo.getDelegateTo());
        return getThreeTier(bo.getInstanceId(), bo.getTaskId());
    }

    // ===== 内部辅助 =====

    private FlowUser findUser(Long taskId, String type) {
        return flwUserMapper.selectOne(new LambdaQueryWrapper<FlowUser>()
            .eq(FlowUser::getAssociated, taskId)
            .eq(FlowUser::getType, type)
            .last("LIMIT 1"));
    }

    private void upsertUser(Long taskId, String type, String processedBy) {
        if (processedBy == null) {
            return;
        }
        FlowUser row = findUser(taskId, type);
        if (row == null) {
            row = new FlowUser();
            row.setAssociated(taskId);
            row.setType(type);
            row.setProcessedBy(processedBy);
            row.setCreateTime(new Date());
            row.setUpdateTime(row.getCreateTime());
            flwUserMapper.insert(row);
        } else {
            row.setProcessedBy(processedBy);
            row.setUpdateTime(new Date());
            flwUserMapper.updateById(row);
        }
    }

    private String readThreeTierFromVariable(String variable) {
        if (variable == null || variable.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(variable);
            JsonNode tier = root.at("/de/ctx/hr/three_tier");
            return tier.isMissingNode() || tier.isNull() ? null : tier.toString();
        } catch (Exception e) {
            log.warn("[hr.threeTier.read] 解析 variable 失败: {}", e.getMessage());
            return null;
        }
    }

    private String writeThreeTierToVariable(String variable, String newJson) {
        try {
            JsonNode root;
            if (variable == null || variable.isBlank()) {
                root = objectMapper.createObjectNode();
            } else {
                root = objectMapper.readTree(variable);
            }
            if (!root.isObject()) {
                throw new ServiceException("variable 顶层不是 object");
            }
            ObjectNode obj = (ObjectNode) root;
            // 走 JSON Pointer 写值（不存在自动建）
            String[] path = THREE_TIER_KEY.split("\\.");
            ObjectNode cursor = obj;
            for (int i = 0; i < path.length - 1; i++) {
                JsonNode next = cursor.get(path[i]);
                if (next == null || !next.isObject()) {
                    ObjectNode created = objectMapper.createObjectNode();
                    cursor.set(path[i], created);
                    cursor = created;
                } else {
                    cursor = (ObjectNode) next;
                }
            }
            cursor.set(path[path.length - 1], objectMapper.readTree(newJson));
            return objectMapper.writeValueAsString(obj);
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            throw new ServiceException("variable JSON 写失败: " + e.getMessage());
        }
    }
}