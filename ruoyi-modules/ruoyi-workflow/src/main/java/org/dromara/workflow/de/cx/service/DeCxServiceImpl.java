package org.dromara.workflow.de.cx.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.dromara.workflow.de.cx.domain.vo.DeCxValidateVo;
import org.dromara.workflow.de.domain.DeSysUserRole;
import org.dromara.workflow.de.mapper.DeSysUserRoleMapper;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.mapper.FlowUserMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 上下文构建与消费 CX 域服务实现（CX-10 · 2026-09-23）。
 *
 * <p>需求（29 号 §五 CX-10）：「上下文使用和提交时校验当前授权、版本和有效期；
 * 验证策略修订号的比较语义」。
 *
 * <h3>三条校验（每条一个判因，fail-fast，不叠加）</h3>
 * <ol>
 *   <li><b>版本</b>：{@code flow_instance.variable} 的 {@code de.ctx.v} 按<b>数值</b>比较
 *       （见 {@link #isOlderRevision(int, int)}），低于服务端最低版本 ⇒ {@code STALE}。</li>
 *   <li><b>授权</b>：当前用户必须在 {@code sys_user_role} 里有角色
 *       （{@link DeSysUserRoleMapper}，<b>实时读库</b>），且与实例有任务关系
 *       （{@code flow_user} 命中为主，见 {@link #hasTaskRelation}）。</li>
 *   <li><b>有效期</b>：{@code de.ctx.expires_at} 存在时须晚于服务端当前时刻 ⇒ 否则 {@code EXPIRED}。</li>
 * </ol>
 *
 * <h3>★ 版本比较语义（本需求的验收要点，单独说明）</h3>
 * <p>验收原句：「<b>版本标识不是简单按字典序判断新旧</b>」。
 * 字典序是错的，反例是具体的：{@code "10".compareTo("9") < 0} ——
 * 即 {@code v=10} 的上下文会被字典序判成<b>比 v=9 旧</b>，于是把新版上下文当旧版拒掉。
 * 本实现把版本读成 {@code int} 后做整数比较（{@link #parseRevision} +
 * {@link #isOlderRevision}），全程不出现字符串比较。
 *
 * <h3>★ 最低版本来自可信服务端事实</h3>
 * <p>最低版本 = {@link #SERVER_MIN_VERSION}（服务端编译期常量），
 * <b>不接受</b>请求体传入（{@code DeCxValidateBo} 刻意没有该字段）——
 * 否则调用方把自己的版本当门槛，「最低版本来自可信服务端事实」这条验收即落空。
 *
 * <h3>★ 为什么授权要实时读 sys_user_role</h3>
 * <p>会话里的 {@code LoginUser.roles} 是登录时快照，撤权后仍持旧值 ⇒
 * 满足不了「<b>撤权后不继续读写</b>」。故本实现在每次校验时读一次
 * {@code sys_user_role}，撤权在下一次校验点即生效。
 *
 * <h3>判定顺序与 fail-closed</h3>
 * <p>顺序：实例存在 → 授权（角色 + 任务关系）→ 上下文可解析 → 版本 → 有效期。
 * 授权放在版本之前，是为了不让无权的调用方从 {@code reason} 里读到实例上下文的版本信息。
 * 版本 / 有效期<b>不可解析一律 fail-closed</b>（拒），不放行 ——
 * 上下文被改坏时不应成为绕过门槛的通道。
 *
 * <p><b>本端点只做校验，不写任何表</b>（无 insert / update / delete）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeCxServiceImpl implements IDeCxService {

    // ===== 判因常量（reason）=====
    /** 三条校验全过 */
    public static final String REASON_OK = "OK";
    /** 实例不存在 */
    public static final String REASON_INSTANCE_NOT_FOUND = "INSTANCE_NOT_FOUND";
    /** 当前用户在 sys_user_role 里没有任何角色（含撤权后的空集） */
    public static final String REASON_NO_ROLE = "NO_ROLE";
    /** 有角色，但与实例无任务关系（flow_user 未命中） */
    public static final String REASON_NO_TASK_RELATION = "NO_TASK_RELATION";
    /** flow_instance.variable 为空 / 缺 de.ctx */
    public static final String REASON_CONTEXT_MISSING = "CONTEXT_MISSING";
    /** flow_instance.variable 不是合法 JSON */
    public static final String REASON_CONTEXT_UNPARSEABLE = "CONTEXT_UNPARSEABLE";
    /** de.ctx.v 缺失（27 号 §5.2：缺 v 即视为不可解析） */
    public static final String REASON_VERSION_MISSING = "VERSION_MISSING";
    /** de.ctx.v 不是整数（如 "abc" / 1.5） */
    public static final String REASON_VERSION_UNPARSEABLE = "VERSION_UNPARSEABLE";
    /** 数值比较下 currentVersion &lt; minVersion */
    public static final String REASON_STALE = "STALE";
    /** de.ctx.expires_at 存在但不是可解析的时刻 */
    public static final String REASON_EXPIRY_UNPARSEABLE = "EXPIRY_UNPARSEABLE";
    /** de.ctx.expires_at 已过 */
    public static final String REASON_EXPIRED = "EXPIRED";

    // ===== 校验时点（action）=====
    /** 上下文使用 */
    public static final String ACTION_READ = "read";
    /** 上下文提交 */
    public static final String ACTION_WRITE = "write";

    /**
     * 服务端最低版本（可信服务端事实）。
     *
     * <p>= {@link DeVariableConstants#SCHEMA_VERSION}（{@code de.ctx.v} 的服务端真值，27 号 §5.2）。
     * 刻意<b>不</b>从请求体取 —— 见类注「最低版本来自可信服务端事实」。
     */
    public static final int SERVER_MIN_VERSION = DeVariableConstants.SCHEMA_VERSION;

    /** de.ctx 里可选的有效期键名（27 号 §5.2 未列该键 ⇒ 只有写入方写了才校验，不强制） */
    private static final String KEY_EXPIRES_AT = "expires_at";

    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwTaskMapper flwTaskMapper;
    private final FlowUserMapper flowUserMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;
    private final DeSysUserRoleMapper deSysUserRoleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public int serverMinVersion() {
        return SERVER_MIN_VERSION;
    }

    @Override
    public DeCxValidateVo contextValidate(Long instanceId, String action, Long userId, String username) {
        if (instanceId == null) {
            throw new ServiceException("CX-10 instanceId 必填");
        }
        if (userId == null) {
            throw new ServiceException("CX-10 当前用户未登录（userId 为空）");
        }
        String act = normalizeAction(action);

        DeCxValidateVo vo = new DeCxValidateVo();
        vo.setInstanceId(instanceId);
        vo.setAction(act);
        vo.setMinVersion(SERVER_MIN_VERSION);
        vo.setCheckedAt(new Date());

        // ① 实例存在性
        FlowInstance inst = flwInstanceMapper.selectById(instanceId);
        if (inst == null) {
            return reject(vo, REASON_INSTANCE_NOT_FOUND);
        }

        // ② 授权 —— 角色（实时读库，撤权即生效）
        List<Long> roleIds = listRoleIds(userId);
        vo.setRoleIds(roleIds);
        if (roleIds.isEmpty()) {
            return reject(vo, REASON_NO_ROLE);
        }
        // ②' 授权 —— 与实例的任务关系（flow_user 命中为主）
        if (!hasTaskRelation(inst, userId, username)) {
            return reject(vo, REASON_NO_TASK_RELATION);
        }

        // ③ 上下文可解析（授权已过 ⇒ 此处才回版本细节，不给无权调用方读版本）
        JsonNode deCtx;
        String variable = inst.getVariable();
        if (variable == null || variable.isBlank()) {
            return reject(vo, REASON_CONTEXT_MISSING);
        }
        try {
            JsonNode root = objectMapper.readTree(variable);
            // 合法 JSON 但不是对象（如 "123" / [1,2]）⇒ 形状不对，不是「缺键」
            if (root == null || !root.isObject()) {
                return reject(vo, REASON_CONTEXT_UNPARSEABLE);
            }
            deCtx = root.get(DeVariableConstants.VARIABLE_KEY);
        } catch (Exception e) {
            log.warn("[cx.contextValidate] variable 解析失败 instanceId={}: {}", instanceId, e.getMessage());
            return reject(vo, REASON_CONTEXT_UNPARSEABLE);
        }
        if (deCtx == null || deCtx.isNull()) {
            return reject(vo, REASON_CONTEXT_MISSING);
        }

        // ④ 版本 —— 数值比较（不是字典序）
        JsonNode vNode = deCtx.get("v");
        if (vNode == null || vNode.isNull()) {
            return reject(vo, REASON_VERSION_MISSING);
        }
        Integer current = parseRevision(vNode);
        if (current == null) {
            return reject(vo, REASON_VERSION_UNPARSEABLE);
        }
        vo.setCurrentVersion(current);
        if (isOlderRevision(current, SERVER_MIN_VERSION)) {
            return reject(vo, REASON_STALE);
        }

        // ⑤ 有效期 —— de.ctx 声明了 expires_at 才校验
        JsonNode expNode = deCtx.get(KEY_EXPIRES_AT);
        if (expNode != null && !expNode.isNull()) {
            Date expiresAt = parseInstant(expNode);
            if (expiresAt == null) {
                return reject(vo, REASON_EXPIRY_UNPARSEABLE);
            }
            vo.setExpiresAt(expiresAt);
            if (expiresAt.before(vo.getCheckedAt())) {
                return reject(vo, REASON_EXPIRED);
            }
        }

        vo.setValid(true);
        vo.setReason(REASON_OK);
        log.info("[cx.contextValidate] PASS instanceId={} action={} userId={} v={} roles={}",
            instanceId, act, userId, current, roleIds.size());
        return vo;
    }

    // ========================================================================
    // ===== 版本语义（CX-10 验收要点：不是字典序）=============================
    // ========================================================================

    /**
     * 版本比较：{@code current} 是否比 {@code min} 旧。
     *
     * <p><b>语义 = 整数比较</b>，不是字典序。反例（本方法要防的错）：
     * {@code "10".compareTo("9") < 0} ⇒ 字典序把 v=10 判成比 v=9 旧；
     * 整数语义下 {@code 10 > 9} ⇒ 新版。
     *
     * <p>本方法只收 {@code int}（调用方先经 {@link #parseRevision} 转数值）——
     * 签名上就没有字符串入口，字典序写不进来。
     *
     * @param current 上下文自身版本（de.ctx.v 的数值形式）
     * @param min     服务端最低版本
     * @return true = 比最低版本旧
     */
    public static boolean isOlderRevision(int current, int min) {
        return current < min;
    }

    /**
     * 把 {@code de.ctx.v} 的 JSON 节点读成<b>整数</b>。
     *
     * <p>接受两种合法形态：JSON 整数（{@code 1}）、纯数字文本（{@code "1"}）。
     * 其余（{@code "abc"} / {@code 1.5} / 布尔 / 对象 / 数组）⇒ {@code null}，
     * 由调用方按 {@code VERSION_UNPARSEABLE} fail-closed。
     *
     * <p><b>注意</b>：文本形态也走 {@link Integer#parseInt} 转成数值再比较 ——
     * 若在此处保留字符串，调用方一旦写 {@code compareTo} 就退化成字典序。
     *
     * @param node {@code de.ctx.v} 节点
     * @return 版本整数；不可解析 = null
     */
    public static Integer parseRevision(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            String raw = node.asText().trim();
            if (raw.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 校验时点归一化：空 = {@code read}；只认 {@code read} / {@code write}。
     *
     * <p>未知取值<b>抛错</b>而不是默认成 read —— 否则把写时点静默降级成读时点，
     * 调用方以为校验过了，实际校验的是另一个时点。
     */
    public static String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return ACTION_READ;
        }
        String a = action.trim().toLowerCase();
        if (!ACTION_READ.equals(a) && !ACTION_WRITE.equals(a)) {
            throw new ServiceException("CX-10 action 非法: " + action + "（只认 read / write）");
        }
        return a;
    }

    // ========================================================================
    // ===== 授权 =============================================================
    // ========================================================================

    /**
     * 当前用户在 {@code sys_user_role} 里的角色 ID 列表（实时读库，去重、保序、剔 null）。
     *
     * <p>空集 = 无角色（含撤权后）⇒ 调用方判 {@code NO_ROLE}。
     */
    private List<Long> listRoleIds(Long userId) {
        List<DeSysUserRole> rows = deSysUserRoleMapper.selectList(
            new LambdaQueryWrapper<DeSysUserRole>().eq(DeSysUserRole::getUserId, userId));
        Set<Long> ids = new LinkedHashSet<>();
        if (rows != null) {
            for (DeSysUserRole r : rows) {
                if (r != null && r.getRoleId() != null) {
                    ids.add(r.getRoleId());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * 当前用户与实例是否有任务关系。
     *
     * <p>三支任一成立即真 —— 与 {@code DeAccessService#hasTaskRelation}
     * （34 号 §2.3 ③）<b>同一语义</b>，两处门禁不各立一套：
     * <ol>
     *   <li><b>待办</b>：实例下当前任务上有该主体的办理权
     *       （{@code flow_user.associated ∈ 实例的 flow_task.id} 且
     *       {@code flow_user.processed_by = 主体}）—— 本需求点名的「flow_user 命中」。</li>
     *   <li><b>已办</b>：该实例的 {@code flow_his_task.approver} = 主体。</li>
     *   <li><b>发起</b>：{@code flow_instance.create_by} = 主体用户名。</li>
     * </ol>
     */
    private boolean hasTaskRelation(FlowInstance inst, Long userId, String username) {
        String uid = String.valueOf(userId);

        // ① 待办：flow_user 命中
        List<FlowTask> tasks = flwTaskMapper.selectList(
            new LambdaQueryWrapper<FlowTask>().eq(FlowTask::getInstanceId, inst.getId()));
        if (tasks != null && !tasks.isEmpty()) {
            Long cnt = flowUserMapper.selectCount(new LambdaQueryWrapper<FlowUser>()
                .in(FlowUser::getAssociated, tasks.stream().map(FlowTask::getId).toList())
                .eq(FlowUser::getProcessedBy, uid));
            if (cnt != null && cnt > 0) {
                return true;
            }
        }

        // ② 已办：flow_his_task.approver = 主体
        Long done = flwHisTaskMapper.selectCount(new LambdaQueryWrapper<FlowHisTask>()
            .eq(FlowHisTask::getInstanceId, inst.getId())
            .eq(FlowHisTask::getApprover, uid));
        if (done != null && done > 0) {
            return true;
        }

        // ③ 发起：flow_instance.create_by = 主体用户名（列型实测 varchar）
        return username != null && !username.isBlank() && username.equals(inst.getCreateBy());
    }

    // ========================================================================
    // ===== 有效期 ===========================================================
    // ========================================================================

    /**
     * 把 {@code de.ctx.expires_at} 读成时刻。
     *
     * <p>接受三种形态（写入方未在 27 号 §5.2 里定该键的类型，故防御式收三种）：
     * 数字 = epoch 毫秒；ISO-8601 带偏移（{@code 2026-09-17T18:40:00+08:00}）；
     * ISO-8601 无偏移（按服务端时区解释）。
     *
     * @return 时刻；不可解析 = null（调用方 fail-closed 判 {@code EXPIRY_UNPARSEABLE}）
     */
    private static Date parseInstant(JsonNode node) {
        if (node.isNumber()) {
            return new Date(node.asLong());
        }
        if (!node.isTextual()) {
            return null;
        }
        String raw = node.asText().trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Date.from(OffsetDateTime.parse(raw).toInstant());
        } catch (Exception ignored) {
            // 落到下一种形态
        }
        try {
            return Date.from(Instant.parse(raw));
        } catch (Exception ignored) {
            // 落到下一种形态
        }
        try {
            return Date.from(LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================================================
    // ===== 内部工具 =========================================================
    // ========================================================================

    /** 置拒因并返回（valid=false）。 */
    private static DeCxValidateVo reject(DeCxValidateVo vo, String reason) {
        vo.setValid(false);
        vo.setReason(reason);
        log.info("[cx.contextValidate] REJECT instanceId={} action={} reason={} v={}",
            vo.getInstanceId(), vo.getAction(), reason, vo.getCurrentVersion());
        return vo;
    }
}
