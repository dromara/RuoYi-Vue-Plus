package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiSkillFile;
import org.dromara.workflow.de.mapper.SaiSkillFileMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_skill_file 服务实现（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL「6.2 Skill 支撑文件内容表」）。
 *
 * <p>唯一键：{@code uk_skill_path(skill_id, file_path)}，新增 / 修改时校验（排除自身 id）；
 * 无逻辑删除列。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiSkillFileServiceImpl
        extends ServiceImpl<SaiSkillFileMapper, SaiSkillFile>
        implements ISaiSkillFileService {

    @Override
    public Long add(SaiSkillFile bo) {
        checkUnique(bo);
        save(bo);
        log.info("[de.sai-skill-file] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiSkillFile bo) {
        checkUnique(bo);
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiSkillFile getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiSkillFile> list(SaiSkillFile bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiSkillFile> page(SaiSkillFile bo) {
        var pg = page(new Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiSkillFile> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiSkillFile>()
            .eq(SaiSkillFile::getSkillId, fkId)
            .orderByDesc(SaiSkillFile::getId));
    }

    /** 按 (skill_id, file_path) 唯一校验；修改时排除自身 id。 */
    private void checkUnique(SaiSkillFile bo) {
        if (bo.getSkillId() != null && bo.getFilePath() != null && !bo.getFilePath().isBlank()) {
            boolean exists = exists(new LambdaQueryWrapper<SaiSkillFile>()
                .eq(SaiSkillFile::getSkillId, bo.getSkillId())
                .eq(SaiSkillFile::getFilePath, bo.getFilePath())
                .ne(bo.getId() != null, SaiSkillFile::getId, bo.getId()));
            if (exists) {
                throw new ServiceException("同一技能下文件路径已存在");
            }
        }
    }

    private LambdaQueryWrapper<SaiSkillFile> buildWrapper(SaiSkillFile bo) {
        LambdaQueryWrapper<SaiSkillFile> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getSkillId() != null) {
                qw.eq(SaiSkillFile::getSkillId, bo.getSkillId());
            }
            if (bo.getFilePath() != null && !bo.getFilePath().isBlank()) {
                qw.like(SaiSkillFile::getFilePath, bo.getFilePath());
            }
        }
        return qw.orderByDesc(SaiSkillFile::getId);
    }
}
