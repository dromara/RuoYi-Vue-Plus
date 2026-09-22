package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.mapper.SaiSkillFileMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sai_skill_file 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiSkillFileServiceImpl
        extends ServiceImpl<SaiSkillFileMapper, Map<String, Object>>
        implements ISaiSkillFileService {

    private static final String STUB_MSG = "sai_skill_file 接口存根，等待 owner 实现";

    @Override
    public Long add(Map<String, Object> bo) {
        log.warn("[de.sai-skill-file] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(Map<String, Object> bo) {
        log.warn("[de.sai-skill-file] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-skill-file] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Map<String, Object> getInfo(Long id) {
        log.warn("[de.sai-skill-file] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<Map<String, Object>> list(Map<String, Object> bo) {
        log.warn("[de.sai-skill-file] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<Map<String, Object>> page(Map<String, Object> bo) {
        log.warn("[de.sai-skill-file] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<Map<String, Object>> listByFk(Long fkId) {
        log.warn("[de.sai-skill-file] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}