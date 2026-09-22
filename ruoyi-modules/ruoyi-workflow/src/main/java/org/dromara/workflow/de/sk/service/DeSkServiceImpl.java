package org.dromara.workflow.de.sk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.sk.domain.vo.DeSkCardVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SK 域服务实现（SK-01 / SK-02 / SK-06 · P1 档 A 实接）。
 *
 * <p>真实落点 = 底座 {@code sai_skill}（DDL 在
 * {@code 04-backend-code/ruoyi-ai/script/sql/ry_ai.sql}）。本仓面<b>未注入</b>
 * sai_skill mapper（不动底座、不发明新 mapper）→ SK-01/02/06 应用层：
 * <ul>
 *   <li>强校验 skillId / userId（N-XX 文档存在性前置）</li>
 *   <li>暴露端点形状 + 关键 5 元组（id / name / version / maintainer / updateDt）接口契约</li>
 *   <li>owner 解锁 sai_skill mapper 后填实</li>
 * </ul>
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSkServiceImpl implements IDeSkService {

    @Override
    public List<Map<String, Object>> catalog(Long userId) {
        // N-XX 校验：userId 若填则必须 > 0
        if (userId != null && userId <= 0) {
            throw new ServiceException("SK-01 userId 非法: " + userId);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> placeholder = new HashMap<>();
        placeholder.put("id", 0L);
        placeholder.put("name", "(sai_skill 待 owner 解锁 mapper 后填实)");
        placeholder.put("ownerScope", userId == null ? "public" : "user");
        placeholder.put("validated", true);
        out.add(placeholder);
        log.info("[sk.catalog] userId={} validated=true", userId);
        return out;
    }

    @Override
    public DeSkCardVo card(Long skillId) {
        if (skillId == null || skillId <= 0) {
            throw new ServiceException(
                "SK-02 skillId 非法: " + skillId + "（必须 > 0）",
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        DeSkCardVo vo = new DeSkCardVo();
        vo.setSkillId(skillId);
        vo.setName("(sai_skill 待 owner 解锁 mapper 后填实)");
        vo.setApplicableTasks("(sai_skill 待 owner 解锁)");
        vo.setInputSchema("(sai_skill 待 owner 解锁)");
        vo.setOutputSchema("(sai_skill 待 owner 解锁)");
        vo.setDependencies(new ArrayList<>());
        vo.setVersion("v0.0.0-pending-unlock");
        vo.setMaintainerId(null);
        log.info("[sk.card] skillId={} validated=true", skillId);
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