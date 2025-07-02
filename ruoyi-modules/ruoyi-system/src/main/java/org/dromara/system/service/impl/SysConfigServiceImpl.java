package org.dromara.system.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.ConfigService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.SysConfig;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.domain.vo.SysConfigVo;
import org.dromara.system.mapper.SysConfigMapper;
import org.dromara.system.service.ISysConfigService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 参数配置 服务层实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysConfigServiceImpl implements ISysConfigService, ConfigService {

    private final SysConfigMapper baseMapper;

    @Override
    public TableDataInfo<SysConfigVo> selectPageConfigList(SysConfigBo config, PageQuery pageQuery) {
        LambdaQueryWrapper<SysConfig> lqw = buildQueryWrapper(config);
        Page<SysConfigVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfigVo selectConfigById(Long configId) {
        return baseMapper.selectVoById(configId);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    @Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "#configKey")
    @Override
    public String selectConfigByKey(String configKey) {
        SysConfig retConfig = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getConfigKey, configKey));
        return ObjectUtils.notNullGetter(retConfig, SysConfig::getConfigValue, StringUtils.EMPTY);
    }

    /**
     * 获取注册开关
     * @param tenantId 租户id
     * @return true开启，false关闭
     */
    @Override
    public boolean selectRegisterEnabled(String tenantId) {
        String configValue = TenantHelper.dynamic(tenantId, () ->
            this.selectConfigByKey("sys.account.registerUser")
        );
        return Convert.toBool(configValue);
    }

    /**
     * 查询参数配置列表
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfigVo> selectConfigList(SysConfigBo config) {
        LambdaQueryWrapper<SysConfig> lqw = buildQueryWrapper(config);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysConfig> buildQueryWrapper(SysConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysConfig> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getConfigName()), SysConfig::getConfigName, bo.getConfigName());
        lqw.eq(StringUtils.isNotBlank(bo.getConfigType()), SysConfig::getConfigType, bo.getConfigType());
        lqw.like(StringUtils.isNotBlank(bo.getConfigKey()), SysConfig::getConfigKey, bo.getConfigKey());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            SysConfig::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByAsc(SysConfig::getConfigId);
        return lqw;
    }

    /**
     * 新增参数配置
     *
     * @param bo 参数配置信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_CONFIG, key = "#bo.configKey")
    @Override
    public String insertConfig(SysConfigBo bo) {
        SysConfig config = MapstructUtils.convert(bo, SysConfig.class);
        int row = baseMapper.insert(config);
        if (row > 0) {
            return config.getConfigValue();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改参数配置
     *
     * @param bo 参数配置信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_CONFIG, key = "#bo.configKey")
    @Override
    public String updateConfig(SysConfigBo bo) {
        int row = 0;
        SysConfig config = MapstructUtils.convert(bo, SysConfig.class);
        if (config.getConfigId() != null) {
            SysConfig temp = baseMapper.selectById(config.getConfigId());
            if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey())) {
                CacheUtils.evict(CacheNames.SYS_CONFIG, temp.getConfigKey());
            }
            row = baseMapper.updateById(config);
        } else {
            CacheUtils.evict(CacheNames.SYS_CONFIG, config.getConfigKey());
            row = baseMapper.update(config, new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, config.getConfigKey()));
        }
        if (row > 0) {
            return config.getConfigValue();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(List<Long> configIds) {
        List<SysConfig> list = baseMapper.selectByIds(configIds);
        list.forEach(config -> {
            if (StringUtils.equals(SystemConstants.YES, config.getConfigType())) {
                throw new ServiceException(String.format("内置参数【%s】不能删除", config.getConfigKey()));
            }
            CacheUtils.evict(CacheNames.SYS_CONFIG, config.getConfigKey());
        });
        baseMapper.deleteByIds(configIds);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache() {
        CacheUtils.clear(CacheNames.SYS_CONFIG);
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfigBo config) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getConfigKey, config.getConfigKey())
            .ne(ObjectUtil.isNotNull(config.getConfigId()), SysConfig::getConfigId, config.getConfigId()));
        return !exist;
    }

    /**
     * 根据参数 key 获取参数值
     *
     * @param configKey 参数 key
     * @return 参数值
     */
    @Override
    public String getConfigValue(String configKey) {
        return SpringUtils.getAopProxy(this).selectConfigByKey(configKey);
    }

}
