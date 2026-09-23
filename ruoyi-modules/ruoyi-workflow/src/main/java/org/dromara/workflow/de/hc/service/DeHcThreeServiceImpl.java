package org.dromara.workflow.de.hc.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.hc.domain.bo.DeHcThreeCategoryBo;
import org.dromara.workflow.de.hc.domain.vo.DeHcThreeCategoryVo;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * HC-03 三分类位落点服务实现（2026-09-23）。
 *
 * <p><b>复用既有表</b>：flow_node.permission_flag（varchar 200 + {@code @@} 分隔
 * + {@code :} 前缀；DDL 自带「多权限」注释）。本类不新造表、不动 flow_task
 * （CLAUDE.md §「最要紧的一条约束」）。
 *
 * <p><b>三分类位 service 串行</b>：COPY → COOPERATE → DELEGATE 拼装成
 * {@code copy:user1,user2@@cooperate:role1@@delegate:dept1} 写入
 * permission_flag 单字段。
 *
 * <p><b>读取</b>：按 {@code @@} 切段，每段以 {@code :} 分键值。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeHcThreeServiceImpl implements IDeHcThreeService {

    private static final String COPY_PREFIX = "copy:";
    private static final String COOPERATE_PREFIX = "cooperate:";
    private static final String DELEGATE_PREFIX = "delegate:";
    private static final String SEP = "@@";

    /**
     * H-3 安全修复（2026-09-23）：白名单正则 —— 用户输入 token 必须匹配此格式才允许拼装。
     *
     * <p>原 {@code buildPermissionFlag} 直接拼接 {@code copyUsers}，攻击者可注入
     * {@code "alice@@cooperate:root"} ⇒ 解析层按 {@code @@} split 后第 2 段以
     * {@code cooperate:} 开头 ⇒ 被识别为合法 cooperate 段 ⇒ 越权流转到 root 角色可触达的所有节点。
     *
     * <p>白名单 = 用户名/角色 key/部门 key 的合理字符集：
     * <ul>
     *   <li>字母数字</li>
     *   <li>{@code _} {@code @} {@code -} {@code .}（用户名/邮箱/部门编号常见）</li>
     *   <li>不含 {@code @@} {@code :} 空白（已通过 splitCsv 前置逗号分隔）</li>
     * </ul>
     */
    private static final Pattern SAFE_TOKEN = Pattern.compile("^[a-zA-Z0-9_@\\-\\.]+$");

    /** 最大 token 数（每个 CSV 拆分后）= 防止 DOS（巨型 list） */
    private static final int MAX_TOKEN_COUNT = 200;
    /** 单 token 最大长度 = 80 字符（DDL varchar(80) 余量） */
    private static final int MAX_TOKEN_LEN = 80;

    private final FlowNodeMapper flowNodeMapper;

    @Override
    public DeHcThreeCategoryVo getThreeCategory(Long nodeId) {
        if (nodeId == null) {
            throw new ServiceException("nodeId 必填");
        }
        FlowNode node = flowNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new ServiceException("节点不存在");
        }
        DeHcThreeCategoryVo vo = new DeHcThreeCategoryVo();
        vo.setNodeId(nodeId);
        vo.setPermissionFlag(node.getPermissionFlag());
        parsePermissionFlag(node.getPermissionFlag(), vo);
        return vo;
    }

    @Override
    public DeHcThreeCategoryVo upsertThreeCategory(DeHcThreeCategoryBo bo) {
        if (bo.getNodeId() == null) {
            throw new ServiceException("nodeId 必填");
        }
        FlowNode node = flowNodeMapper.selectById(bo.getNodeId());
        if (node == null) {
            throw new ServiceException("节点不存在");
        }
        String flag;
        if (bo.getPermissionFlag() != null && !bo.getPermissionFlag().isBlank()) {
            // H-3 修复：传入整段 permission_flag 时仍要走白名单格式校验
            flag = sanitizeWholeFlag(bo.getPermissionFlag());
        } else {
            // H-3 修复：分别对 copyUsers / cooperateRoles / delegateDepts 做转义 + 白名单
            flag = buildPermissionFlag(
                sanitizeCsvTokens("copyUsers", bo.getCopyUsers()),
                sanitizeCsvTokens("cooperateRoles", bo.getCooperateRoles()),
                sanitizeCsvTokens("delegateDepts", bo.getDelegateDepts()));
        }
        node.setPermissionFlag(flag);
        // 用 LambdaUpdateWrapper 限定 id，避免 updateById 对所有列全更新
        int rows = flowNodeMapper.update(node,
            new LambdaUpdateWrapper<FlowNode>().eq(FlowNode::getId, bo.getNodeId()));
        log.info("[hc.threeCategory] nodeId={} flag={} rows={}", bo.getNodeId(), flag, rows);
        return getThreeCategory(bo.getNodeId());
    }

    // ===== 内部辅助 =====

    /**
     * 清洗 CSV token 输入（H-3 修复后 · 2026-09-23）。
     *
     * <p>每个 token 必须匹配 {@link #SAFE_TOKEN}：
     * <ul>
     *   <li>含 {@code @@} / {@code :} / 空白 / 其他特殊字符 ⇒ 拒绝（抛 ServiceException）</li>
     *   <li>超过 {@link #MAX_TOKEN_LEN} 字符 ⇒ 拒绝</li>
     *   <li>CSV 拆分后 token 数超过 {@link #MAX_TOKEN_COUNT} ⇒ 拒绝（防 DOS）</li>
     * </ul>
     */
    private String sanitizeCsvTokens(String fieldName, String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        // 1) 拒绝显式的 @@（即使在单 token 内也是 H-3 攻击 payload 的载体）
        if (csv.contains(SEP)) {
            throw new ServiceException(fieldName + " 禁止包含分隔符 @@，请用半角逗号分隔 token");
        }
        // 2) 拒绝显式的 :（即使在单 token 内也是 H-3 攻击 payload 的载体）
        if (csv.contains(":")) {
            throw new ServiceException(fieldName + " 禁止包含冒号 :，前缀由系统拼装");
        }
        // 3) 拆分 + 白名单
        String[] tokens = csv.split(",");
        if (tokens.length > MAX_TOKEN_COUNT) {
            throw new ServiceException(fieldName + " token 数超限（最多 " + MAX_TOKEN_COUNT + "）");
        }
        List<String> safe = new ArrayList<>(tokens.length);
        for (String t : tokens) {
            String trimmed = t.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_TOKEN_LEN) {
                throw new ServiceException(fieldName + " 单 token 超长（≤" + MAX_TOKEN_LEN + " 字符）");
            }
            if (!SAFE_TOKEN.matcher(trimmed).matches()) {
                throw new ServiceException(fieldName + " 含非法字符 token=\"" + trimmed
                    + "\"（仅允许字母数字 + _ @ - .）");
            }
            safe.add(trimmed);
        }
        return String.join(",", safe);
    }

    /**
     * 整段 permission_flag 白名单校验（H-3 修复后 · 2026-09-23）。
     *
     * <p>格式必须匹配 {@code ^(copy|cooperate|delegate):[a-zA-Z0-9_,@\\-\\.]+(@@(copy|cooperate|delegate):[a-zA-Z0-9_,@\\-\\.]+)*$}
     * —— 每段必须以三个合法前缀之一开头，token 不含 {@code @@} / {@code :}。
     */
    private static final Pattern WHOLE_FLAG = Pattern.compile(
        "^(copy|cooperate|delegate):[a-zA-Z0-9_@\\-\\.]+"
        + "(@@(copy|cooperate|delegate):[a-zA-Z0-9_@\\-\\.]+)*$");

    private String sanitizeWholeFlag(String flag) {
        String trimmed = flag.trim();
        if (trimmed.length() > 200) {
            throw new ServiceException("permission_flag 超过 200 字符（DDL varchar(200)）");
        }
        if (!WHOLE_FLAG.matcher(trimmed).matches()) {
            throw new ServiceException("permission_flag 格式非法：必须 (copy|cooperate|delegate):token"
                + " 段以 @@ 分隔；token 仅允许字母数字 + _ @ - .；不允许空白 / 多余 : / 多余 @@");
        }
        // 190 字符业务层上限（DDL 200 字符留 10 字符余量给 race condition 写入）
        if (trimmed.length() > 190) {
            throw new ServiceException("permission_flag 业务上限 ≤190 字符（DDL 200 字符留 10 字符余量）");
        }
        return trimmed;
    }

    private String buildPermissionFlag(String copyUsers, String cooperateRoles, String delegateDepts) {
        StringBuilder sb = new StringBuilder();
        if (copyUsers != null && !copyUsers.isBlank()) {
            sb.append(COPY_PREFIX).append(copyUsers).append(SEP);
        }
        if (cooperateRoles != null && !cooperateRoles.isBlank()) {
            sb.append(COOPERATE_PREFIX).append(cooperateRoles).append(SEP);
        }
        if (delegateDepts != null && !delegateDepts.isBlank()) {
            sb.append(DELEGATE_PREFIX).append(delegateDepts).append(SEP);
        }
        String s = sb.toString();
        if (s.endsWith(SEP)) {
            s = s.substring(0, s.length() - SEP.length());
        }
        if (s.length() > 200) {
            throw new ServiceException("permission_flag 拼装后超过 200 字符限制（DDL varchar(200)）");
        }
        return s;
    }

    private void parsePermissionFlag(String flag, DeHcThreeCategoryVo vo) {
        if (flag == null || flag.isBlank()) {
            vo.setCopyUsers(Collections.emptyList());
            vo.setCooperateRoles(Collections.emptyList());
            vo.setDelegateDepts(Collections.emptyList());
            return;
        }
        List<String> copy = null, cooperate = null, delegate = null;
        for (String seg : flag.split(SEP)) {
            if (seg.startsWith(COPY_PREFIX)) {
                copy = splitCsv(seg.substring(COPY_PREFIX.length()));
            } else if (seg.startsWith(COOPERATE_PREFIX)) {
                cooperate = splitCsv(seg.substring(COOPERATE_PREFIX.length()));
            } else if (seg.startsWith(DELEGATE_PREFIX)) {
                delegate = splitCsv(seg.substring(DELEGATE_PREFIX.length()));
            } else {
                // H-3 联动修复（L-2）：拒绝未识别 prefix 的段（防止 H-3 攻击 payload 残留静默）
                log.warn("[hc.threeCategory.parse] 未识别的 permission_flag 段：{}", seg);
                throw new ServiceException("permission_flag 包含未识别前缀段：" + seg
                    + "（仅允许 copy/cooperate/delegate）");
            }
        }
        vo.setCopyUsers(copy == null ? Collections.emptyList() : copy);
        vo.setCooperateRoles(cooperate == null ? Collections.emptyList() : cooperate);
        vo.setDelegateDepts(delegate == null ? Collections.emptyList() : delegate);
    }

    private List<String> splitCsv(String s) {
        if (s == null || s.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(s.split(","))
            .map(String::trim)
            .filter(x -> !x.isEmpty())
            .toList();
    }
}