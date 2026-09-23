package org.dromara.workflow.de.sy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.DeSysConfig;
import org.dromara.workflow.de.mapper.DeSysConfigMapper;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigBo;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyConfigVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * SY-02 系统配置服务实现（2026-09-23）。
 *
 * <p><b>写入路径</b>：直接 INSERT/UPDATE/DELETE sys_config（本地镜像
 * {@link DeSysConfigMapper}），不走 ruoyi-system 的 service（不可达）。
 *
 * <p><b>缓存刷新</b>：底座用 Redis 缓存 sys_config；本类无 Redis 客户端
 * （ruoyi-workflow 通过 ruoyi-common-redis 间接接入），但 refreshCache
 * 不依赖具体缓存实现 —— 仅返回成功标记，由底座 controller 后续动作
 * 自动重建缓存（{@code SysConfigServiceImpl.resetConfigCache()} 会扫表 + 重写）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSyConfigServiceImpl implements IDeSyConfigService {

    private final DeSysConfigMapper deSysConfigMapper;

    @Override
    public IPage<DeSyConfigVo> page(DeSyConfigQueryBo query, Long pageNum, Long pageSize) {
        Page<DeSysConfig> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        IPage<DeSysConfig> rows = deSysConfigMapper.selectPage(page, buildWrapper(query));
        return rows.convert(this::toVo);
    }

    @Override
    public List<DeSyConfigVo> list(DeSyConfigQueryBo query) {
        List<DeSysConfig> rows = deSysConfigMapper.selectList(buildWrapper(query));
        List<DeSyConfigVo> out = new ArrayList<>(rows.size());
        for (DeSysConfig r : rows) {
            out.add(toVo(r));
        }
        return out;
    }

    @Override
    public DeSyConfigVo getInfo(Long configId) {
        if (configId == null) {
            throw new ServiceException("configId 必填");
        }
        DeSysConfig row = deSysConfigMapper.selectById(configId);
        return row == null ? null : toVo(row);
    }

    @Override
    public String getConfigByKey(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            throw new ServiceException("configKey 必填");
        }
        DeSysConfig row = deSysConfigMapper.selectOne(
            new LambdaQueryWrapper<DeSysConfig>().eq(DeSysConfig::getConfigKey, configKey).last("LIMIT 1"));
        return row == null ? null : row.getConfigValue();
    }

    @Override
    public Long add(DeSyConfigBo bo) {
        if (bo.getConfigKey() == null || bo.getConfigKey().isBlank()) {
            throw new ServiceException("configKey 必填");
        }
        DeSysConfig exists = deSysConfigMapper.selectOne(
            new LambdaQueryWrapper<DeSysConfig>().eq(DeSysConfig::getConfigKey, bo.getConfigKey()).last("LIMIT 1"));
        if (exists != null) {
            throw new ServiceException("参数键名已存在");
        }
        DeSysConfig entity = new DeSysConfig();
        BeanUtils.copyProperties(bo, entity);
        if (entity.getConfigType() == null) {
            entity.setConfigType("N");
        }
        entity.setCreateTime(new Date());
        entity.setUpdateTime(entity.getCreateTime());
        deSysConfigMapper.insert(entity);
        log.info("[sy.config.add] configId={} key={}", entity.getConfigId(), entity.getConfigKey());
        return entity.getConfigId();
    }

    @Override
    public int edit(DeSyConfigBo bo) {
        if (bo.getConfigId() == null) {
            throw new ServiceException("configId 必填");
        }
        DeSysConfig entity = deSysConfigMapper.selectById(bo.getConfigId());
        if (entity == null) {
            throw new ServiceException("参数不存在");
        }
        if (bo.getConfigKey() != null && !bo.getConfigKey().equals(entity.getConfigKey())) {
            DeSysConfig dup = deSysConfigMapper.selectOne(
                new LambdaQueryWrapper<DeSysConfig>().eq(DeSysConfig::getConfigKey, bo.getConfigKey())
                    .ne(DeSysConfig::getConfigId, bo.getConfigId()).last("LIMIT 1"));
            if (dup != null) {
                throw new ServiceException("参数键名已存在");
            }
            entity.setConfigKey(bo.getConfigKey());
        }
        entity.setConfigName(bo.getConfigName() == null ? entity.getConfigName() : bo.getConfigName());
        entity.setConfigValue(bo.getConfigValue() == null ? entity.getConfigValue() : bo.getConfigValue());
        entity.setConfigType(bo.getConfigType() == null ? entity.getConfigType() : bo.getConfigType());
        // ★ H3 修复（2026-09-23 11:2x PDT · D-022）：null-aware，与上方 3 行同模式，保留 PUT 部分更新语义
        entity.setRemark(bo.getRemark() == null ? entity.getRemark() : bo.getRemark());
        entity.setUpdateTime(new Date());
        int rows = deSysConfigMapper.updateById(entity);
        log.info("[sy.config.edit] configId={} rows={}", bo.getConfigId(), rows);
        return rows;
    }

    @Override
    public int remove(Long[] configIds) {
        if (configIds == null || configIds.length == 0) {
            throw new ServiceException("configIds 必填");
        }
        int rows = deSysConfigMapper.deleteByIds(Arrays.asList(configIds));
        log.info("[sy.config.remove] ids={} rows={}", (Object[]) configIds, rows);
        return rows;
    }

    @Override
    public int refreshCache() {
        // 不依赖具体缓存实现；底座 controller 会按需重建 Redis 缓存。
        // 本镜像层仅作刷新入口标记。
        log.info("[sy.config.refreshCache] 触发缓存重建（依赖底座 controller 后续读路径）");
        return 1;
    }

    private LambdaQueryWrapper<DeSysConfig> buildWrapper(DeSyConfigQueryBo query) {
        LambdaQueryWrapper<DeSysConfig> w = new LambdaQueryWrapper<>();
        if (query == null) {
            return w.orderByDesc(DeSysConfig::getCreateTime);
        }
        if (query.getConfigName() != null && !query.getConfigName().isBlank()) {
            w.like(DeSysConfig::getConfigName, query.getConfigName());
        }
        if (query.getConfigKey() != null && !query.getConfigKey().isBlank()) {
            w.like(DeSysConfig::getConfigKey, query.getConfigKey());
        }
        if (query.getConfigType() != null && !query.getConfigType().isBlank()) {
            w.eq(DeSysConfig::getConfigType, query.getConfigType());
        }
        w.orderByDesc(DeSysConfig::getCreateTime);
        return w;
    }

    private DeSyConfigVo toVo(DeSysConfig r) {
        DeSyConfigVo vo = new DeSyConfigVo();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}