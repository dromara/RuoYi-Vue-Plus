package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiModelProvider;
import org.dromara.workflow.de.mapper.SaiModelProviderMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_model_provider 服务实现（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL「2.1 AI 模型提供商表」）。
 *
 * <p>唯一键：{@code uk_provider_name(provider_name)}、{@code uk_provider_key(provider_key)}，
 * 新增 / 修改时按列校验（排除自身 id）；无逻辑删除列。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiModelProviderServiceImpl
        extends ServiceImpl<SaiModelProviderMapper, SaiModelProvider>
        implements ISaiModelProviderService {

    @Override
    public Long add(SaiModelProvider bo) {
        checkUnique(bo);
        save(bo);
        log.info("[de.sai-model-provider] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiModelProvider bo) {
        checkUnique(bo);
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiModelProvider getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiModelProvider> list(SaiModelProvider bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiModelProvider> page(SaiModelProvider bo) {
        var pg = page(new Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiModelProvider> listByFk(Long fkId) {
        // 本表无外键列，by-fk 语义不适用，返回空列表
        return List.of();
    }

    /** 按 provider_name / provider_key 唯一校验；修改时排除自身 id。 */
    private void checkUnique(SaiModelProvider bo) {
        Long excludeId = bo.getId();
        if (bo.getProviderName() != null && !bo.getProviderName().isBlank()) {
            boolean exists = exists(new LambdaQueryWrapper<SaiModelProvider>()
                .eq(SaiModelProvider::getProviderName, bo.getProviderName())
                .ne(excludeId != null, SaiModelProvider::getId, excludeId));
            if (exists) {
                throw new ServiceException("提供商名称已存在");
            }
        }
        if (bo.getProviderKey() != null && !bo.getProviderKey().isBlank()) {
            boolean exists = exists(new LambdaQueryWrapper<SaiModelProvider>()
                .eq(SaiModelProvider::getProviderKey, bo.getProviderKey())
                .ne(excludeId != null, SaiModelProvider::getId, excludeId));
            if (exists) {
                throw new ServiceException("提供商标识符已存在");
            }
        }
    }

    private LambdaQueryWrapper<SaiModelProvider> buildWrapper(SaiModelProvider bo) {
        LambdaQueryWrapper<SaiModelProvider> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getProviderName() != null && !bo.getProviderName().isBlank()) {
                qw.like(SaiModelProvider::getProviderName, bo.getProviderName());
            }
            if (bo.getProviderKey() != null && !bo.getProviderKey().isBlank()) {
                qw.eq(SaiModelProvider::getProviderKey, bo.getProviderKey());
            }
            if (bo.getIsEnabled() != null) {
                qw.eq(SaiModelProvider::getIsEnabled, bo.getIsEnabled());
            }
        }
        return qw.orderByDesc(SaiModelProvider::getId);
    }
}
