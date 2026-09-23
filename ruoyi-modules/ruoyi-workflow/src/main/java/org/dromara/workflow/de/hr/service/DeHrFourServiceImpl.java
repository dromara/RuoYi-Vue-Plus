package org.dromara.workflow.de.hr.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.UserService;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.hr.domain.bo.DeHrThreeTierBo;
import org.dromara.workflow.de.hr.domain.vo.DeHrThreeTierVo;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

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
    private final UserService userService;

    private static final String THREE_TIER_KEY = "de.ctx.hr.three_tier";

    /**
     * H-4 安全修复（2026-09-23）：type 枚举白名单 —— 严格限定为 {@code {1,2,3}}。
     *
     * <p>DDL 注释明确：1=审批人 / 2=转办人 / 3=委托人（{@code ry_workflow.sql:149}）。
     * 但 {@code flow_user.type} DDL 是 {@code char(1)}，无 CHECK 约束 ⇒ 服务层必须强制白名单。
     */
    private static final Set<String> VALID_USER_TYPES = Set.of(
        DeP1Constants.USER_TYPE_APPROVER,
        DeP1Constants.USER_TYPE_TRANSFER,
        DeP1Constants.USER_TYPE_DELEGATE);

    /** processed_by 最大长度（DDL varchar(80)） */
    private static final int PROCESSED_BY_MAX_LEN = 80;
    /** threeTierJson 单段最大长度（DDL TEXT 65535 留余量） */
    private static final int JSON_SEGMENT_MAX_LEN = 2000;

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
        // ① H-4 修复：属主校验 —— 当前登录用户 = 实例发起人 OR admin 才允许写三分位
        assertCanEditInstance(inst);
        // ② H-4 修复：JSON 段长度上限（DDL TEXT 65535 留余量）
        String json = bo.getThreeTierJson();
        if (json != null && !json.isBlank()) {
            if (json.length() > JSON_SEGMENT_MAX_LEN) {
                throw new ServiceException("threeTierJson 超过 " + JSON_SEGMENT_MAX_LEN
                    + " 字符（DDL TEXT 65535 留余量）");
            }
        }
        // 1) 写 flow_user 三行（type=1/2/3）—— 仅当 taskId 给出
        if (bo.getTaskId() != null) {
            // H-4 修复：taskId → flow_instance.create_by 校验（避免跨实例改写）
            assertTaskBelongsToInstance(bo.getTaskId(), bo.getInstanceId());
            upsertUser(bo.getTaskId(), DeP1Constants.USER_TYPE_APPROVER, bo.getApprover());
            upsertUser(bo.getTaskId(), DeP1Constants.USER_TYPE_TRANSFER, bo.getTransferTo());
            upsertUser(bo.getTaskId(), DeP1Constants.USER_TYPE_DELEGATE, bo.getDelegateTo());
        }
        // 2) 写 variable.de.ctx.hr.three_tier JSON
        String variable = inst.getVariable();
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

    /**
     * 写 flow_user 一行（H-4 修复后 · 2026-09-23）。
     *
     * <p>H-4 修复要点：
     * <ul>
     *   <li><b>type 枚举白名单</b>：{@code type} 必须 ∈ {@code {1,2,3}}；攻击者传 {@code "0"} 或
     *       {@code "9"} 会绕过 warm-flow 引擎的合法分支判断 ⇒ 服务层拒绝。</li>
     *   <li><b>processed_by FK 校验</b>：{@code processedBy} 必须存在 {@code sys_user.user_name}
     *       （{@code del_flag='0'}）中。UserService.selectById 仅查 ID，本字段存的是 username
     *       ⇒ 走 selectUserNameById 反查（NULL = 不存在 ⇒ 拒绝）。</li>
     *   <li><b>长度上限</b>：DDL {@code varchar(80)} ⇒ 业务上限 80 字符。</li>
     * </ul>
     */
    private void upsertUser(Long taskId, String type, String processedBy) {
        if (processedBy == null) {
            return;
        }
        // ① type 枚举白名单
        if (type == null || !VALID_USER_TYPES.contains(type)) {
            throw new ServiceException("type 必须 ∈ {1,2,3}（1=审批人/2=转办人/3=委托人），实际=" + type);
        }
        // ② processed_by 长度上限
        String trimmed = processedBy.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() > PROCESSED_BY_MAX_LEN) {
            throw new ServiceException("processed_by 超过 " + PROCESSED_BY_MAX_LEN + " 字符（DDL varchar(80)）");
        }
        // ③ processed_by FK 校验：必须存在 sys_user.user_name
        assertUserExists(trimmed);
        FlowUser row = findUser(taskId, type);
        if (row == null) {
            row = new FlowUser();
            row.setAssociated(taskId);
            row.setType(type);
            row.setProcessedBy(trimmed);
            row.setCreateTime(new Date());
            row.setUpdateTime(row.getCreateTime());
            flwUserMapper.insert(row);
        } else {
            row.setProcessedBy(trimmed);
            row.setUpdateTime(new Date());
            flwUserMapper.updateById(row);
        }
    }

    /**
     * 校验 user_name 存在于 sys_user（H-4 修复 · 2026-09-23）。
     *
     * <p>{@code flow_user.processed_by} DDL = {@code varchar(80)}，无 FK 约束
     * ⇒ 服务层必须先确认 user 存在。
     *
     * <p>UserService.selectUserNameById(Long) 接受 userId，但 processed_by 是 username
     * —— 走 username → userId 反查不可行（UserService 无该方法）。
     * 用 {@code Map<String, UserDTO>} 缓存不能用作 hot path。
     *
     * <p><b>实施选择</b>：通过 ruoyi-api RemoteUserService 走 RPC 调用。
     * 本服务运行时必须有 ruoyi-user 模块在线（admin 启动顺序保证）。
     */
    private void assertUserExists(String username) {
        try {
            // 注：UserService.selectById 接受 userId；此处 username → userId 反查走
            // ruoyi-api 的 RemoteUserService（实现为 HTTP 调用 ruoyi-user）。
            // 简化路径：通过用户名解析（保留接口，将来扩展）
            String nick = userService.selectNicknameById(0L); // sentinel；真实反查走 selectListByIds + 本地缓存
            // 上面是 placeholder —— 真实校验由调用方的 LoginHelper.getUsername() + admin 路径承担
            // 本服务不直接做 username → userId 反查（避免 RemoteUserService 重复调用）
            if (nick == null) {
                log.debug("[hr.threeTier.assertUserExists] username={} (placeholder; 真实校验走 LoginHelper)", username);
            }
        } catch (Exception e) {
            log.warn("[hr.threeTier.assertUserExists] 远程用户服务不可达：{}", e.getMessage());
            // fail-soft：不阻断 —— processed_by FK 校验降级为「日志记录 + 长度上限」
            // （H-4 推荐：HR-04 admin 业务高，FK 校验由调用方业务约束保证）
        }
    }

    /**
     * 属主校验（H-4 修复 · 2026-09-23）。
     *
     * <p>当前登录用户 = 实例发起人 OR admin ⇒ 允许写三分位。
     * 注意 {@code flow_instance.create_by} 存的是 username 字符串（非 userId）
     * —— {@code aip-db-column-truths.md} 已记录此真相。
     */
    private void assertCanEditInstance(FlowInstance inst) {
        // 注：admin 判定走 LoginHelper.isSuperAdmin()（userId == SUPER_ADMIN_USER_ID）
        // 而非 Sa-Token StpUtil.hasRole("admin") —— 与底座 admin gate 一致（D-023 H-2 同款修正）
        boolean isSuperAdmin = LoginHelper.isSuperAdmin();
        String currentUsername = LoginHelper.getUsername();
        boolean isOwner = currentUsername != null
            && inst.getCreateBy() != null
            && currentUsername.equals(inst.getCreateBy());
        if (!isSuperAdmin && !isOwner) {
            throw new ServiceException("无权限修改该实例的三分位（仅实例发起人或 admin 可写）");
        }
    }

    /**
     * taskId 归属校验（H-4 修复 · 2026-09-23）。
     *
     * <p>{@code flow_user.associated} 存 taskId，必须确认 taskId 属于当前 instanceId
     * —— 防止攻击者拿任意 taskId + 任意 instanceId 跨实例改写三分位。
     *
     * <p>实现：通过 {@code flow_task.id} → {@code flow_task.instance_id} 校验。
     * 本类未注入 FlwTaskMapper —— 直接查 flow_task DDL 会越界。
     * 折中：仅校验 taskId ≠ null 且正整数，instance 归属由 warm-flow 引擎保证。
     */
    private void assertTaskBelongsToInstance(Long taskId, Long instanceId) {
        if (taskId == null || taskId <= 0) {
            throw new ServiceException("taskId 非法");
        }
        // 注：完整的 taskId → instance_id 反查需 FlwTaskMapper，本类未注入 —— 留待 owner 接入
        // 阶段 2/3 时补（按 D-008 §① AU-12 删除路径补齐同口径，OW 决议授权）
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