package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    // ★ D-010 批⑥实化（2026-09-23）：typed 存根 → 真实 CRUD

    @Override
    public Long add(SaiAgentSkill bo) {
        save(bo);
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiAgentSkill bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(java.util.Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiAgentSkill getInfo(Long id) {
        return getById(id);
    }

    @Override
    public java.util.List<SaiAgentSkill> list(SaiAgentSkill bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiAgentSkill> page(SaiAgentSkill bo) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public java.util.List<SaiAgentSkill> listByFk(Long fkId) {
        return java.util.Collections.emptyList();
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaiAgentSkill> buildWrapper(SaiAgentSkill bo) {
        return new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaiAgentSkill>().orderByDesc(SaiAgentSkill::getId);
    }
}
