package org.dromara.workflow.de.ag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.ag.domain.vo.DeAgOverviewVo;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AG 域服务实现。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeAgServiceImpl implements IDeAgService {

    private final SaiAgentMapper saiAgentMapper;
    private final SaiUserAgentMapper saiUserAgentMapper;

    @Override
    public DeAgOverviewVo overview(Long agentId) {
        SaiAgent agent = saiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new ServiceException("数字员工不存在: id=" + agentId);
        }
        DeAgOverviewVo vo = new DeAgOverviewVo();
        vo.setAgentId(agent.getId());
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setAvatar(agent.getAvatar());
        vo.setInstruction(agent.getInstruction());
        vo.setStatus(agent.getStatus());
        vo.setMemoryEnabled(agent.getMemoryEnabled());
        vo.setRagEnabled(agent.getRagEnabled());
        vo.setUpdateDt(agent.getUpdateDt());
        // 能力数占位：1 = skill 启用
        vo.setCapabilityCount(agent.getSkillEnabled() == null ? 0 : agent.getSkillEnabled());
        // 任务总数占位（AU-13 待 owner 裁）
        vo.setTaskTotal(0L);
        return vo;
    }

    @Override
    public List<String> relations(Long agentId, Long userId) {
        SaiAgent agent = saiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new ServiceException("数字员工不存在: id=" + agentId);
        }
        List<String> rels = new ArrayList<>();
        // 我创建的
        if (userId != null && userId.equals(agent.getCreatorId())) {
            rels.add("created-by-me");
        }
        // 共享给我的（已订阅）
        if (userId != null) {
            Long c = saiUserAgentMapper.selectCount(
                new LambdaQueryWrapper<SaiUserAgent>()
                    .eq(SaiUserAgent::getUserId, userId)
                    .eq(SaiUserAgent::getAgentId, agentId)
            );
            if (c != null && c > 0) {
                rels.add("shared-with-me");
            }
        }
        // 团队使用（判定 = 是否被订阅超过 1 人）
        Long subs = saiUserAgentMapper.selectCount(
            new LambdaQueryWrapper<SaiUserAgent>().eq(SaiUserAgent::getAgentId, agentId)
        );
        if (subs != null && subs > 1) {
            rels.add("team-usage");
        }
        return rels;
    }
}