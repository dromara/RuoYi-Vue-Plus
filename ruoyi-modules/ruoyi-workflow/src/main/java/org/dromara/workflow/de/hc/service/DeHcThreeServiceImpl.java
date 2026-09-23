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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
            flag = bo.getPermissionFlag();
        } else {
            flag = buildPermissionFlag(bo.getCopyUsers(), bo.getCooperateRoles(), bo.getDelegateDepts());
        }
        node.setPermissionFlag(flag);
        // 用 LambdaUpdateWrapper 限定 id，避免 updateById 对所有列全更新
        int rows = flowNodeMapper.update(node,
            new LambdaUpdateWrapper<FlowNode>().eq(FlowNode::getId, bo.getNodeId()));
        log.info("[hc.threeCategory] nodeId={} flag={} rows={}", bo.getNodeId(), flag, rows);
        return getThreeCategory(bo.getNodeId());
    }

    // ===== 内部辅助 =====

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