package org.dromara.pms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.exception.ServiceException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.dromara.pms.domain.bo.PmsRoomRoomBo;
import org.dromara.pms.domain.vo.PmsRoomRoomVo;
import org.dromara.pms.domain.PmsRoomRoom;
import org.dromara.pms.mapper.PmsRoomRoomMapper;
import org.dromara.pms.service.IPmsRoomRoomService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Date;

/**
 * 房间管理Service业务层处理
 *
 * @author xuhf
 * @date 2025-05-28
 */
@RequiredArgsConstructor
@Service
public class PmsRoomRoomServiceImpl implements IPmsRoomRoomService {

    private final PmsRoomRoomMapper baseMapper;

    /**
     * 查询房间管理
     */
    @Override
    public PmsRoomRoomVo queryById(Long roomId) {
        return baseMapper.selectVoByIdWithDetails(roomId);
    }

    /**
     * 查询房间管理列表
     */
    @Override
    public TableDataInfo<PmsRoomRoomVo> queryPageList(PmsRoomRoomBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsRoomRoom> lqw = buildQueryWrapper(bo);
        Page<PmsRoomRoomVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询房间管理列表
     */
    @Override
    public List<PmsRoomRoomVo> queryList(PmsRoomRoomBo bo) {
        LambdaQueryWrapper<PmsRoomRoom> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsRoomRoom> buildQueryWrapper(PmsRoomRoomBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsRoomRoom> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, PmsRoomRoom::getDeptId, bo.getDeptId());
        lqw.eq(bo.getRoomTypeId() != null, PmsRoomRoom::getRoomTypeId, bo.getRoomTypeId());
        lqw.like(StringUtils.isNotBlank(bo.getRoomNumber()), PmsRoomRoom::getRoomNumber, bo.getRoomNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getFloor()), PmsRoomRoom::getFloor, bo.getFloor());
        lqw.eq(StringUtils.isNotBlank(bo.getRoomStatus()), PmsRoomRoom::getRoomStatus, bo.getRoomStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getCleaningStatus()), PmsRoomRoom::getCleaningStatus, bo.getCleaningStatus());
        lqw.orderByAsc(PmsRoomRoom::getSortOrder).orderByAsc(PmsRoomRoom::getRoomNumber);
        return lqw;
    }

    /**
     * 新增房间管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsRoomRoomBo bo) {
        PmsRoomRoom add = MapstructUtils.convert(bo, PmsRoomRoom.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setRoomId(add.getRoomId());
        }
        return flag;
    }

    /**
     * 修改房间管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsRoomRoomBo bo) {
        PmsRoomRoom update = MapstructUtils.convert(bo, PmsRoomRoom.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsRoomRoom entity) {
        // 检查房间号在同一门店内的唯一性
        if (StringUtils.isNotBlank(entity.getRoomNumber()) && entity.getDeptId() != null) {
            PmsRoomRoom existRoom = baseMapper.selectByRoomNumber(
                    entity.getRoomNumber(), entity.getDeptId(), entity.getRoomId());
            if (ObjectUtil.isNotNull(existRoom)) {
                throw new ServiceException("房间号在当前门店已存在");
            }
        }

        // 校验入住人数逻辑
        if (entity.getRoomTypeId() != null) {
            // 这里可以添加房型相关的校验逻辑
        }
    }

    /**
     * 批量删除房间管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否存在关联的锁定记录
            for (Long id : ids) {
                // 这里可以添加业务校验逻辑
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据房型ID查询房间列表
     */
    @Override
    public List<PmsRoomRoomVo> queryByRoomTypeId(Long roomTypeId) {
        return baseMapper.selectByRoomTypeId(roomTypeId);
    }

    /**
     * 根据部门ID查询房间列表
     */
    @Override
    public List<PmsRoomRoomVo> queryByDeptId(Long deptId) {
        return baseMapper.selectByDeptId(deptId);
    }

    /**
     * 根据房间状态查询房间列表
     */
    @Override
    public List<PmsRoomRoomVo> queryByStatus(Long deptId, String roomStatus, String cleaningStatus) {
        return baseMapper.selectByStatus(deptId, roomStatus, cleaningStatus);
    }

    /**
     * 更新房间状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoomStatus(Long roomId, String roomStatus, String statusRemarks) {
        PmsRoomRoom room = new PmsRoomRoom();
        room.setRoomId(roomId);
        room.setRoomStatus(roomStatus);
        room.setStatusRemarks(statusRemarks);
        return baseMapper.updateById(room) > 0;
    }

    /**
     * 更新清洁状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCleaningStatus(Long roomId, String cleaningStatus) {
        PmsRoomRoom room = new PmsRoomRoom();
        room.setRoomId(roomId);
        room.setCleaningStatus(cleaningStatus);
        if ("clean".equals(cleaningStatus)) {
            room.setLastCleaningTime(new Date());
        }
        return baseMapper.updateById(room) > 0;
    }

    /**
     * 批量更新房间状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateRoomStatus(List<Long> roomIds, String roomStatus, String statusRemarks) {
        for (Long roomId : roomIds) {
            updateRoomStatus(roomId, roomStatus, statusRemarks);
        }
        return true;
    }

    /**
     * 批量更新清洁状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateCleaningStatus(List<Long> roomIds, String cleaningStatus) {
        for (Long roomId : roomIds) {
            updateCleaningStatus(roomId, cleaningStatus);
        }
        return true;
    }
}
