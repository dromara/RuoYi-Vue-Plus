package com.ruoyi.system.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.annotation.DataSource;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.DataSourceType;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 参数配置 服务层实现
 *
 * @author ruoyi
 */
@Service
public class SysConfigServiceImpl extends ServicePlusImpl<SysConfigMapper, SysConfig, SysConfig> implements ISysConfigService {

	@Autowired
	private RedisCache redisCache;

	/**
	 * 项目启动时，初始化参数到缓存
	 */
	@PostConstruct
	public void init() {
		loadingConfigCache();
	}

	@Override
	public TableDataInfo<SysConfig> selectPageConfigList(SysConfig config) {
		Map<String, Object> params = config.getParams();
		LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<SysConfig>()
			.like(StrUtil.isNotBlank(config.getConfigName()), SysConfig::getConfigName, config.getConfigName())
			.eq(StrUtil.isNotBlank(config.getConfigType()), SysConfig::getConfigType, config.getConfigType())
			.like(StrUtil.isNotBlank(config.getConfigKey()), SysConfig::getConfigKey, config.getConfigKey())
			.apply(Validator.isNotEmpty(params.get("beginTime")),
				"date_format(create_time,'%y%m%d') >= date_format({0},'%y%m%d')",
				params.get("beginTime"))
			.apply(Validator.isNotEmpty(params.get("endTime")),
				"date_format(create_time,'%y%m%d') <= date_format({0},'%y%m%d')",
				params.get("endTime"));
		return PageUtils.buildDataInfo(page(PageUtils.buildPage(), lqw));
	}

	/**
	 * 查询参数配置信息
	 *
	 * @param configId 参数配置ID
	 * @return 参数配置信息
	 */
	@Override
	@DataSource(DataSourceType.MASTER)
	public SysConfig selectConfigById(Long configId) {
		return baseMapper.selectById(configId);
	}

	/**
	 * 根据键名查询参数配置信息
	 *
	 * @param configKey 参数key
	 * @return 参数键值
	 */
	@Override
	public String selectConfigByKey(String configKey) {
		String configValue = Convert.toStr(redisCache.getCacheObject(getCacheKey(configKey)));
		if (Validator.isNotEmpty(configValue)) {
			return configValue;
		}
		SysConfig retConfig = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
			.eq(SysConfig::getConfigKey, configKey));
		if (Validator.isNotNull(retConfig)) {
			redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
			return retConfig.getConfigValue();
		}
		return StrUtil.EMPTY;
	}

	/**
	 * 获取验证码开关
	 *
	 * @return true开启，false关闭
	 */
	@Override
	public boolean selectCaptchaOnOff() {
		String captchaOnOff = selectConfigByKey("sys.account.captchaOnOff");
		if (StrUtil.isEmpty(captchaOnOff)) {
			return true;
		}
		return Convert.toBool(captchaOnOff);
	}

	/**
	 * 查询参数配置列表
	 *
	 * @param config 参数配置信息
	 * @return 参数配置集合
	 */
	@Override
	public List<SysConfig> selectConfigList(SysConfig config) {
		Map<String, Object> params = config.getParams();
		LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<SysConfig>()
			.like(StrUtil.isNotBlank(config.getConfigName()), SysConfig::getConfigName, config.getConfigName())
			.eq(StrUtil.isNotBlank(config.getConfigType()), SysConfig::getConfigType, config.getConfigType())
			.like(StrUtil.isNotBlank(config.getConfigKey()), SysConfig::getConfigKey, config.getConfigKey())
			.apply(Validator.isNotEmpty(params.get("beginTime")),
				"date_format(create_time,'%y%m%d') >= date_format({0},'%y%m%d')",
				params.get("beginTime"))
			.apply(Validator.isNotEmpty(params.get("endTime")),
				"date_format(create_time,'%y%m%d') <= date_format({0},'%y%m%d')",
				params.get("endTime"));
		return baseMapper.selectList(lqw);
	}

	/**
	 * 新增参数配置
	 *
	 * @param config 参数配置信息
	 * @return 结果
	 */
	@Override
	public int insertConfig(SysConfig config) {
		int row = baseMapper.insert(config);
		if (row > 0) {
			redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
		}
		return row;
	}

	/**
	 * 修改参数配置
	 *
	 * @param config 参数配置信息
	 * @return 结果
	 */
	@Override
	public int updateConfig(SysConfig config) {
		int row = baseMapper.updateById(config);
		if (row > 0) {
			redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
		}
		return row;
	}

	/**
	 * 批量删除参数信息
	 *
	 * @param configIds 需要删除的参数ID
	 * @return 结果
	 */
	@Override
	public void deleteConfigByIds(Long[] configIds) {
		for (Long configId : configIds) {
			SysConfig config = selectConfigById(configId);
			if (StrUtil.equals(UserConstants.YES, config.getConfigType())) {
				throw new CustomException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
			}
			redisCache.deleteObject(getCacheKey(config.getConfigKey()));
		}
		baseMapper.deleteBatchIds(Arrays.asList(configIds));
	}

	/**
	 * 加载参数缓存数据
	 */
	@Override
	public void loadingConfigCache() {
		List<SysConfig> configsList = selectConfigList(new SysConfig());
		for (SysConfig config : configsList) {
			redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
		}
	}

	/**
	 * 清空参数缓存数据
	 */
	@Override
	public void clearConfigCache() {
		Collection<String> keys = redisCache.keys(Constants.SYS_CONFIG_KEY + "*");
		redisCache.deleteObject(keys);
	}

	/**
	 * 重置参数缓存数据
	 */
	@Override
	public void resetConfigCache() {
		clearConfigCache();
		loadingConfigCache();
	}

	/**
	 * 校验参数键名是否唯一
	 *
	 * @param config 参数配置信息
	 * @return 结果
	 */
	@Override
	public String checkConfigKeyUnique(SysConfig config) {
		Long configId = Validator.isNull(config.getConfigId()) ? -1L : config.getConfigId();
		SysConfig info = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, config.getConfigKey()));
		if (Validator.isNotNull(info) && info.getConfigId().longValue() != configId.longValue()) {
			return UserConstants.NOT_UNIQUE;
		}
		return UserConstants.UNIQUE;
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
