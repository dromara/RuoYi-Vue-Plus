package org.dromara.pms.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.dromara.pms.domain.bo.PmsRoomTypeBo;
import org.dromara.pms.domain.vo.PmsRoomTypeVo;
import org.dromara.pms.domain.PmsRoomType;
import org.dromara.pms.mapper.PmsRoomTypeMapper;
import org.dromara.pms.service.IPmsRoomTypeService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;

/**
 * 房型管理Service业务层处理
 *
 * @author xuhf
 * @date 2025-05-28
 */
@RequiredArgsConstructor
@Service
public class PmsRoomTypeServiceImpl implements IPmsRoomTypeService {

    private final PmsRoomTypeMapper baseMapper;

    /**
     * 查询房型管理
     */
    @Override
    public PmsRoomTypeVo queryById(Long roomTypeId) {
        return baseMapper.selectVoByIdWithDept(roomTypeId);
    }

    /**
     * 查询房型管理列表
     */
    @Override
    public TableDataInfo<PmsRoomTypeVo> queryPageList(PmsRoomTypeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsRoomType> lqw = buildQueryWrapper(bo);
        Page<PmsRoomTypeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询房型管理列表
     */
    @Override
    public List<PmsRoomTypeVo> queryList(PmsRoomTypeBo bo) {
        LambdaQueryWrapper<PmsRoomType> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsRoomType> buildQueryWrapper(PmsRoomTypeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsRoomType> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, PmsRoomType::getDeptId, bo.getDeptId());
        lqw.like(StringUtils.isNotBlank(bo.getTypeName()), PmsRoomType::getTypeName, bo.getTypeName());
        lqw.eq(StringUtils.isNotBlank(bo.getTypeCode()), PmsRoomType::getTypeCode, bo.getTypeCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsRoomType::getStatus, bo.getStatus());
        lqw.orderByAsc(PmsRoomType::getSortOrder);
        lqw.orderByAsc(PmsRoomType::getRoomTypeId);
        return lqw;
    }

    /**
     * 新增房型管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsRoomTypeBo bo) {
        // 校验房型代码唯一性
        if (!checkTypeCodeUnique(bo.getTypeCode(), bo.getDeptId(), null)) {
            throw new RuntimeException("房型代码已存在");
        }

        // 校验最大入住人数不能小于标准入住人数
        if (bo.getMaxOccupancy() < bo.getStandardOccupancy()) {
            throw new RuntimeException("最大入住人数不能小于标准入住人数");
        }

        PmsRoomType add = MapstructUtils.convert(bo, PmsRoomType.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setRoomTypeId(add.getRoomTypeId());
        }
        return flag;
    }

    /**
     * 修改房型管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsRoomTypeBo bo) {
        // 校验房型代码唯一性
        if (!checkTypeCodeUnique(bo.getTypeCode(), bo.getDeptId(), bo.getRoomTypeId())) {
            throw new RuntimeException("房型代码已存在");
        }

        // 校验最大入住人数不能小于标准入住人数
        if (bo.getMaxOccupancy() < bo.getStandardOccupancy()) {
            throw new RuntimeException("最大入住人数不能小于标准入住人数");
        }

        PmsRoomType update = MapstructUtils.convert(bo, PmsRoomType.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsRoomType entity) {
        // 设置默认排序值
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }

        // 设置默认状态
        if (StringUtils.isBlank(entity.getStatus())) {
            entity.setStatus("active");
        }
    }

    /**
     * 批量删除房型管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否存在关联的房间
            for (Long id : ids) {
                // TODO: 检查是否有关联的房间
                // 这里可以添加业务校验逻辑
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 根据部门ID查询房型列表
     */
    @Override
    public List<PmsRoomTypeVo> queryByDeptId(Long deptId) {
        return baseMapper.selectByDeptId(deptId);
    }

    /**
     * 校验房型代码唯一性
     */
    @Override
    public Boolean checkTypeCodeUnique(String typeCode, Long deptId, Long roomTypeId) {
        PmsRoomType roomType = baseMapper.selectByTypeCode(typeCode, deptId, roomTypeId);
        return roomType == null;
    }

    /**
     * 获取房型选项列表（用于下拉选择）
     */
    @Override
    public List<Map<String, Object>> getOptions(Long deptId) {
        List<PmsRoomTypeVo> roomTypes = queryByDeptId(deptId);
        return roomTypes.stream().map(roomType -> {
            Map<String, Object> option = new HashMap<>();
            option.put("label", roomType.getTypeName());
            option.put("value", roomType.getRoomTypeId());
            return option;
        }).toList();
    }
}
