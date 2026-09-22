package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiSkill;
import org.dromara.workflow.de.mapper.SaiSkillMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_skill 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：{@link SaiSkill}（既有 domain POJO，BR-05 校验需要的 3 列）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiSkillServiceImpl
        extends ServiceImpl<SaiSkillMapper, SaiSkill>
        implements ISaiSkillService {

    private static final String STUB_MSG = "sai_skill 接口存根，等待 owner 实现";

    @Override
    public Long add(SaiSkill bo) {
        log.warn("[de.sai-skill] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(SaiSkill bo) {
        log.warn("[de.sai-skill] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-skill] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public SaiSkill getInfo(Long id) {
        log.warn("[de.sai-skill] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiSkill> list(SaiSkill bo) {
        log.warn("[de.sai-skill] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<SaiSkill> page(SaiSkill bo) {
        log.warn("[de.sai-skill] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiSkill> listByFk(Long fkId) {
        log.warn("[de.sai-skill] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}