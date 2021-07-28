package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.entity.SysDictType;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.mapper.SysDictTypeMapper;
import com.ruoyi.system.service.ISysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 字典 业务层处理
 *
 * @author ruoyi
 */
@Service
public class SysDictTypeServiceImpl extends ServicePlusImpl<SysDictTypeMapper, SysDictType, SysDictType> implements ISysDictTypeService {

	@Autowired
	private SysDictDataMapper dictDataMapper;

	/**
	 * 项目启动时，初始化字典到缓存
	 */
	@PostConstruct
	public void init() {
		loadingDictCache();
	}

	@Override
	public TableDataInfo<SysDictType> selectPageDictTypeList(SysDictType dictType) {
		Map<String, Object> params = dictType.getParams();
		LambdaQueryWrapper<SysDictType> lqw = new LambdaQueryWrapper<SysDictType>()
			.like(StrUtil.isNotBlank(dictType.getDictName()), SysDictType::getDictName, dictType.getDictName())
			.eq(StrUtil.isNotBlank(dictType.getStatus()), SysDictType::getStatus, dictType.getStatus())
			.like(StrUtil.isNotBlank(dictType.getDictType()), SysDictType::getDictType, dictType.getDictType())
			.apply(Validator.isNotEmpty(params.get("beginTime")),
				"date_format(create_time,'%y%m%d') >= date_format({0},'%y%m%d')",
				params.get("beginTime"))
			.apply(Validator.isNotEmpty(params.get("endTime")),
				"date_format(create_time,'%y%m%d') <= date_format({0},'%y%m%d')",
				params.get("endTime"));
		return PageUtils.buildDataInfo(page(PageUtils.buildPage(), lqw));
	}

	/**
	 * 根据条件分页查询字典类型
	 *
	 * @param dictType 字典类型信息
	 * @return 字典类型集合信息
	 */
	@Override
	public List<SysDictType> selectDictTypeList(SysDictType dictType) {
		Map<String, Object> params = dictType.getParams();
		return list(new LambdaQueryWrapper<SysDictType>()
			.like(StrUtil.isNotBlank(dictType.getDictName()), SysDictType::getDictName, dictType.getDictName())
			.eq(StrUtil.isNotBlank(dictType.getStatus()), SysDictType::getStatus, dictType.getStatus())
			.like(StrUtil.isNotBlank(dictType.getDictType()), SysDictType::getDictType, dictType.getDictType())
			.apply(Validator.isNotEmpty(params.get("beginTime")),
				"date_format(create_time,'%y%m%d') >= date_format({0},'%y%m%d')",
				params.get("beginTime"))
			.apply(Validator.isNotEmpty(params.get("endTime")),
				"date_format(create_time,'%y%m%d') <= date_format({0},'%y%m%d')",
				params.get("endTime")));
	}

	/**
	 * 根据所有字典类型
	 *
	 * @return 字典类型集合信息
	 */
	@Override
	public List<SysDictType> selectDictTypeAll() {
		return list();
	}

	/**
	 * 根据字典类型查询字典数据
	 *
	 * @param dictType 字典类型
	 * @return 字典数据集合信息
	 */
	@Override
	public List<SysDictData> selectDictDataByType(String dictType) {
		List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
		if (CollUtil.isNotEmpty(dictDatas)) {
			return dictDatas;
		}
		dictDatas = dictDataMapper.selectDictDataByType(dictType);
		if (CollUtil.isNotEmpty(dictDatas)) {
			DictUtils.setDictCache(dictType, dictDatas);
			return dictDatas;
		}
		return null;
	}

	/**
	 * 根据字典类型ID查询信息
	 *
	 * @param dictId 字典类型ID
	 * @return 字典类型
	 */
	@Override
	public SysDictType selectDictTypeById(Long dictId) {
		return getById(dictId);
	}

	/**
	 * 根据字典类型查询信息
	 *
	 * @param dictType 字典类型
	 * @return 字典类型
	 */
	@Override
	public SysDictType selectDictTypeByType(String dictType) {
		return getOne(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
	}

	/**
	 * 批量删除字典类型信息
	 *
	 * @param dictIds 需要删除的字典ID
	 * @return 结果
	 */
	@Override
	public void deleteDictTypeByIds(Long[] dictIds) {
		for (Long dictId : dictIds) {
			SysDictType dictType = selectDictTypeById(dictId);
			if (dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
				.eq(SysDictData::getDictType, dictType.getDictType())) > 0) {
				throw new CustomException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
			}
			DictUtils.removeDictCache(dictType.getDictType());
		}
		baseMapper.deleteBatchIds(Arrays.asList(dictIds));
	}

	/**
	 * 加载字典缓存数据
	 */
	@Override
	public void loadingDictCache() {
		List<SysDictType> dictTypeList = list();
		for (SysDictType dictType : dictTypeList) {
			List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dictType.getDictType());
			DictUtils.setDictCache(dictType.getDictType(), dictDatas);
		}
	}

	/**
	 * 清空字典缓存数据
	 */
	@Override
	public void clearDictCache() {
		DictUtils.clearDictCache();
	}

	/**
	 * 重置字典缓存数据
	 */
	@Override
	public void resetDictCache() {
		clearDictCache();
		loadingDictCache();
	}

	/**
	 * 新增保存字典类型信息
	 *
	 * @param dict 字典类型信息
	 * @return 结果
	 */
	@Override
	public int insertDictType(SysDictType dict) {
		int row = baseMapper.insert(dict);
		if (row > 0) {
			DictUtils.setDictCache(dict.getDictType(), null);
		}
		return row;
	}

	/**
	 * 修改保存字典类型信息
	 *
	 * @param dict 字典类型信息
	 * @return 结果
	 */
	@Override
	@Transactional
	public int updateDictType(SysDictType dict) {
		SysDictType oldDict = getById(dict.getDictId());
		dictDataMapper.update(null, new LambdaUpdateWrapper<SysDictData>()
			.set(SysDictData::getDictType, dict.getDictType())
			.eq(SysDictData::getDictType, oldDict.getDictType()));
		int row = baseMapper.updateById(dict);
		if (row > 0) {
			List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dict.getDictType());
			DictUtils.setDictCache(dict.getDictType(), dictDatas);
		}
		return row;
	}

	/**
	 * 校验字典类型称是否唯一
	 *
	 * @param dict 字典类型
	 * @return 结果
	 */
	@Override
	public String checkDictTypeUnique(SysDictType dict) {
		Long dictId = Validator.isNull(dict.getDictId()) ? -1L : dict.getDictId();
		SysDictType dictType = getOne(new LambdaQueryWrapper<SysDictType>()
			.eq(SysDictType::getDictType, dict.getDictType())
			.last("limit 1"));
		if (Validator.isNotNull(dictType) && dictType.getDictId().longValue() != dictId.longValue()) {
			return UserConstants.NOT_UNIQUE;
		}
		return UserConstants.UNIQUE;
	}
}
