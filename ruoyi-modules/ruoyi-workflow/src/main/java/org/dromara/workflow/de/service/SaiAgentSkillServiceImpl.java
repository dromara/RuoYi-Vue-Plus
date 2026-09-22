package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentSkill;
import org.dromara.workflow.de.mapper.SaiAgentSkillMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_skill 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：{@link SaiAgentSkill}（既有 domain POJO，BR-05 校验需要的 3 列）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiAgentSkillServiceImpl
        extends ServiceImpl<SaiAgentSkillMapper, SaiAgentSkill>
        implements ISaiAgentSkillService {

    private static final String STUB_MSG = "sai_agent_skill 接口存根，等待 owner 实现";

    @Override
    public Long add(SaiAgentSkill bo) {
        log.warn("[de.sai-agent-skill] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(SaiAgentSkill bo) {
        log.warn("[de.sai-agent-skill] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-agent-skill] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public SaiAgentSkill getInfo(Long id) {
        log.warn("[de.sai-agent-skill] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiAgentSkill> list(SaiAgentSkill bo) {
        log.warn("[de.sai-agent-skill] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<SaiAgentSkill> page(SaiAgentSkill bo) {
        log.warn("[de.sai-agent-skill] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiAgentSkill> listByFk(Long fkId) {
        log.warn("[de.sai-agent-skill] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}