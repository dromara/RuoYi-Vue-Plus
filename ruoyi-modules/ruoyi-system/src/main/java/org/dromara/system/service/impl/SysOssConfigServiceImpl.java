package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.domain.SysOssConfig;
import org.dromara.system.domain.bo.SysOssConfigBo;
import org.dromara.system.domain.vo.SysOssConfigVo;
import org.dromara.system.mapper.SysOssConfigMapper;
import org.dromara.system.service.ISysOssConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 对象存储配置Service业务层处理
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

    private final SysOssConfigMapper ossConfigMapper;

    /**
     * 项目启动时，初始化参数到缓存，加载配置类
     */
    @Override
    public void init() {
        List<SysOssConfig> list = ossConfigMapper.selectList();
        // 加载OSS初始化配置
        for (SysOssConfig config : list) {
            String configKey = config.getConfigKey();
            if (SystemConstants.YES.equals(config.getStatus())) {
                RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, configKey);
            }
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
    }

    /**
     * 查询对象存储配置详情。
     *
     * @param ossConfigId 配置主键
     * @return 对象存储配置详情
     */
    @Override
    public SysOssConfigVo queryById(Long ossConfigId) {
        return ossConfigMapper.selectVoById(ossConfigId);
    }

    /**
     * 分页查询对象存储配置列表。
     *
     * @param bo        配置筛选条件
     * @param pageQuery 分页参数
     * @return 配置分页结果
     */
    @Override
    public PageResult<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOssConfig> lqw = buildQueryWrapper(bo);
        Page<SysOssConfigVo> result = ossConfigMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }


    /**
     * 构造对象存储配置列表查询条件。
     *
     * @param bo 配置筛选条件
     * @return 包含配置标识、桶名称和状态条件的查询包装器
     */
    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        return QueryBuilder.lambda(SysOssConfig.class)
            .eqIfText(SysOssConfig::getConfigKey, bo.getConfigKey())
            .likeIfText(SysOssConfig::getBucketName, bo.getBucketName())
            .eqIfText(SysOssConfig::getStatus, bo.getStatus())
            .orderByAsc(SysOssConfig::getOssConfigId)
            .build();
    }

    /**
     * 新增对象存储配置并刷新缓存。
     *
     * @param bo 配置业务对象
     * @return 新增成功返回 {@code true}
     */
    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        boolean flag = ossConfigMapper.insert(config) > 0;
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
        return flag;
    }

    /**
     * 更新对象存储配置并刷新缓存。
     *
     * @param bo 配置业务对象
     * @return 更新成功返回 {@code true}
     */
    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        boolean flag = ossConfigMapper.lambda()
            .set(ObjectUtil.isNull(config.getPrefix()), SysOssConfig::getPrefix, "")
            .set(ObjectUtil.isNull(config.getRegion()), SysOssConfig::getRegion, "")
            .set(ObjectUtil.isNull(config.getExt1()), SysOssConfig::getExt1, "")
            .set(ObjectUtil.isNull(config.getRemark()), SysOssConfig::getRemark, "")
            .eq(SysOssConfig::getOssConfigId, config.getOssConfigId())
            .update(config);
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 待保存的对象存储配置实体
     */
    private void validEntityBeforeSave(SysOssConfig entity) {
        if (StringUtils.isNotEmpty(entity.getConfigKey())
            && !checkConfigKeyUnique(entity)) {
            throw new ServiceException("操作配置'{}'失败, 配置key已存在!", entity.getConfigKey());
        }
    }

    /**
     * 删除对象存储配置并同步清理缓存。
     *
     * @param ids     主键集合
     * @param isValid 是否执行业务校验
     * @return 删除成功返回 {@code true}
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (CollUtil.containsAny(ids, OssConstant.SYSTEM_DATA_IDS)) {
                throw new ServiceException("系统内置, 不可删除!");
            }
        }
        List<SysOssConfig> list = CollUtil.newArrayList();
        for (Long configId : ids) {
            SysOssConfig config = ossConfigMapper.selectById(configId);
            list.add(config);
        }
        boolean flag = ossConfigMapper.deleteByIds(ids) > 0;
        if (flag) {
            list.forEach(sysOssConfig ->
                CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, sysOssConfig.getConfigKey()));
        }
        return flag;
    }

    /**
     * 判断configKey是否唯一
     *
     * @param sysOssConfig 对象存储配置实体
     * @return 唯一返回 {@code true}
     */
    private boolean checkConfigKeyUnique(SysOssConfig sysOssConfig) {
        long ossConfigId = ObjectUtils.notNull(sysOssConfig.getOssConfigId(), -1L);
        SysOssConfig info = ossConfigMapper.lambda()
            .select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
            .eq(SysOssConfig::getConfigKey, sysOssConfig.getConfigKey())
            .one();
        if (ObjectUtil.isNotNull(info) && info.getOssConfigId() != ossConfigId) {
            return false;
        }
        return true;
    }

    /**
     * 启用禁用状态
     *
     * @param bo 配置业务对象
     * @return 更新条数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateOssConfigStatus(SysOssConfigBo bo) {
        SysOssConfig sysOssConfig = MapstructUtils.convert(bo, SysOssConfig.class);
        int row = ossConfigMapper.lambda().set(SysOssConfig::getStatus, SystemConstants.NO).updateCount();
        row += ossConfigMapper.updateById(sysOssConfig);
        if (row > 0) {
            RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, sysOssConfig.getConfigKey());
        }
        return row;
    }

}
