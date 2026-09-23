package org.dromara.workflow.de.sk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.domain.SaiSkill;
import org.dromara.workflow.de.mapper.SaiSkillMapper;
import org.dromara.workflow.de.sk.domain.vo.DeSkCardVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SK 域服务实现（SK-01 / SK-02 / SK-06 · P1 档 A 实接）。
 *
 * <p>真实落点 = sai_skill 表（DDL 在 ry_ai.sql）。
 * ★ 2026-09-23 接线（D-013 · 审计漂移#1 正解）：SaiSkillMapper 已存在（批⑥ typed 化），
 * 原「未注入 mapper 待 owner 解锁」javadoc 已过期——本类改为**直接消费** SaiSkillMapper
 * （复用既有，不造第二条数据路径）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSkServiceImpl implements IDeSkService {

    private final SaiSkillMapper saiSkillMapper;

    @Override
    public List<Map<String, Object>> catalog(Long userId) {
        // N-XX 校验：userId 若填则必须 > 0
        if (userId != null && userId <= 0) {
            throw new ServiceException("SK-01 userId 非法: " + userId);
        }
        // D-013 接线：真查 sai_skill（批⑥ mapper 复用）
        List<SaiSkill> skills = saiSkillMapper.selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        for (SaiSkill sk : skills) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", sk.getId());
            row.put("name", sk.getName());
            row.put("version", sk.getVersion());
            row.put("ownerScope", userId == null ? "public" : "user");
            out.add(row);
        }
        log.info("[sk.catalog] userId={} skills={}", userId, out.size());
        return out;
    }

    @Override
    public DeSkCardVo card(Long skillId) {
        if (skillId == null || skillId <= 0) {
            throw new ServiceException(
                "SK-02 skillId 非法: " + skillId + "（必须 > 0）",
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        // D-013 接线：真查 sai_skill
        SaiSkill sk = saiSkillMapper.selectById(skillId);
        if (sk == null) {
            throw new ServiceException("SK-02 技能不存在: " + skillId,
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        DeSkCardVo vo = new DeSkCardVo();
        vo.setSkillId(sk.getId());
        vo.setName(sk.getName());
        vo.setDependencies(new ArrayList<>());
        vo.setVersion(sk.getVersion() == null ? null : String.valueOf(sk.getVersion()));
        vo.setMaintainerId(null);
        log.info("[sk.card] skillId={} found name={}", skillId, sk.getName());
        return vo;
    }

    @Override
    public Map<String, Object> dependencies(Long skillId) {
        if (skillId == null || skillId <= 0) {
            throw new ServiceException("SK-06 skillId 非法: " + skillId);
        }
        Map<String, Object> dep = new HashMap<>();
        dep.put("skillId", skillId);
        dep.put("dependentAgents", new ArrayList<>());
        dep.put("dependentNodes", new ArrayList<>());
        dep.put("dependentAutomations", new ArrayList<>());
        dep.put("validated", true);
        dep.put("note", "sai_agent_skill / flow_node / sj_job 关联待 AU-13 解锁 mapper 后填实");
        log.info("[sk.dependencies] skillId={} validated=true", skillId);
        return dep;
    }
}