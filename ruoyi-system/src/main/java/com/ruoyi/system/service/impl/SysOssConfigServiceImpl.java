package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.oss.constant.CloudConstant;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.bo.SysOssConfigBo;
import com.ruoyi.system.domain.vo.SysOssConfigVo;
import com.ruoyi.system.domain.SysOssConfig;
import com.ruoyi.system.mapper.SysOssConfigMapper;
import com.ruoyi.system.service.ISysOssConfigService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 云存储配置Service业务层处理
 *
 * @author ruoyi
 * @date 2021-08-11
 */
@Service
public class SysOssConfigServiceImpl extends ServicePlusImpl<SysOssConfigMapper, SysOssConfig, SysOssConfigVo> implements ISysOssConfigService {

	@Autowired
	private ISysConfigService iSysConfigService;
	@Autowired
	private SysConfigMapper sysConfigMapper;
	@Autowired
	private RedisCache redisCache;

    @Override
    public SysOssConfigVo queryById(Integer ossConfigId){
        return getVoById(ossConfigId);
    }

    @Override
    public TableDataInfo<SysOssConfigVo> queryPageList(SysOssConfigBo bo) {
        PagePlus<SysOssConfig, SysOssConfigVo> result = pageVo(PageUtils.buildPagePlus(), buildQueryWrapper(bo));
        return PageUtils.buildDataInfo(result);
    }


    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysOssConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getConfigKey()), SysOssConfig::getConfigKey, bo.getConfigKey());
        lqw.like(StringUtils.isNotBlank(bo.getBucketName()), SysOssConfig::getBucketName, bo.getBucketName());
        return lqw;
    }

    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig add = BeanUtil.toBean(bo, SysOssConfig.class);
        validEntityBeforeSave(add);
        return save(add);
    }

    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig update = BeanUtil.toBean(bo, SysOssConfig.class);
        validEntityBeforeSave(update);
        return updateById(update);
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(SysOssConfig entity){
        //TODO 做一些数据校验,如唯一约束
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid) {
        return removeByIds(ids);
    }

	/**
	 * 判断configKey是否唯一
	 * @param bo
	 * @return
	 */
	@Override
	public String checkConfigKeyUnique(SysOssConfigBo bo) {
		Long ossConfigId = StringUtils.isNull(bo.getOssConfigId()) ? -1L : bo.getOssConfigId();
		SysOssConfig info = getOne(new LambdaQueryWrapper<SysOssConfig>()
			.select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
			.eq(SysOssConfig::getConfigKey, bo.getConfigKey()).last("limit 1"));
		if (StringUtils.isNotNull(info) && info.getOssConfigId().longValue() != ossConfigId.longValue()) {
			return UserConstants.NOT_UNIQUE;
		}
		return UserConstants.UNIQUE;
	}

	/**
	 * 启用禁用状态
	 * @param sysOssConfig
	 * @return
	 */
	@Override
	public int updateOssConfigStatus(SysOssConfig sysOssConfig) {
    	LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
    	queryWrapper.eq(SysConfig::getConfigKey, CloudConstant.CLOUD_STORAGE_CONFIG_KEY);
    	SysConfig sysConfig = sysConfigMapper.selectOne(queryWrapper);

    	if(ObjectUtil.isNotNull(sysConfig)){
    		sysConfig.setConfigValue(sysOssConfig.getConfigKey());
    		iSysConfigService.updateConfig(sysConfig);
		}
		return baseMapper.updateById(sysOssConfig);
	}

	/**
	 * 设置cache key
	 *
	 * @param configKey 参数键
	 * @return 缓存键key
	 */
	private String getCacheKey(String configKey) {
		return Constants.SYS_CONFIG_KEY + configKey;
	}


}
