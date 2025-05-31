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
import org.dromara.pms.domain.bo.PmsRoomLockBo;
import org.dromara.pms.domain.vo.PmsRoomLockVo;
import org.dromara.pms.domain.PmsRoomLock;
import org.dromara.pms.mapper.PmsRoomLockMapper;
import org.dromara.pms.service.IPmsRoomLockService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Date;

/**
 * 房间锁定管理Service业务层处理
 *
 * @author xuhf
 * @date 2025-05-28
 */
@RequiredArgsConstructor
@Service
public class PmsRoomLockServiceImpl implements IPmsRoomLockService {

    private final PmsRoomLockMapper baseMapper;

    /**
     * 查询房间锁定管理
     */
    @Override
    public PmsRoomLockVo queryById(Long lockId) {
        return baseMapper.selectVoByIdWithDetails(lockId);
    }

    /**
     * 查询房间锁定管理列表
     */
    @Override
    public TableDataInfo<PmsRoomLockVo> queryPageList(PmsRoomLockBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsRoomLock> lqw = buildQueryWrapper(bo);
        Page<PmsRoomLockVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询房间锁定管理列表
     */
    @Override
    public List<PmsRoomLockVo> queryList(PmsRoomLockBo bo) {
        LambdaQueryWrapper<PmsRoomLock> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsRoomLock> buildQueryWrapper(PmsRoomLockBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsRoomLock> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, PmsRoomLock::getDeptId, bo.getDeptId());
        lqw.eq(bo.getRoomId() != null, PmsRoomLock::getRoomId, bo.getRoomId());
        lqw.eq(StringUtils.isNotBlank(bo.getLockType()), PmsRoomLock::getLockType, bo.getLockType());
        lqw.eq(StringUtils.isNotBlank(bo.getLockStatus()), PmsRoomLock::getLockStatus, bo.getLockStatus());
        lqw.ge(bo.getLockStartTime() != null, PmsRoomLock::getLockStartTime, bo.getLockStartTime());
        lqw.le(bo.getLockEndTime() != null, PmsRoomLock::getLockEndTime, bo.getLockEndTime());
        lqw.orderByDesc(PmsRoomLock::getLockStartTime);
        return lqw;
    }

    /**
     * 新增房间锁定管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsRoomLockBo bo) {
        PmsRoomLock add = MapstructUtils.convert(bo, PmsRoomLock.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setLockId(add.getLockId());
        }
        return flag;
    }

    /**
     * 修改房间锁定管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsRoomLockBo bo) {
        PmsRoomLock update = MapstructUtils.convert(bo, PmsRoomLock.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsRoomLock entity) {
        // 校验锁定时间
        if (entity.getLockStartTime() != null && entity.getLockEndTime() != null) {
            if (entity.getLockStartTime().after(entity.getLockEndTime())) {
                throw new ServiceException("锁定开始时间不能晚于结束时间");
            }
        }

        // 校验时间冲突
        if (entity.getRoomId() != null && entity.getLockStartTime() != null) {
            Boolean hasConflict = checkTimeConflict(
                    entity.getRoomId(),
                    entity.getLockStartTime(),
                    entity.getLockEndTime(),
                    entity.getLockId());
            if (hasConflict) {
                throw new ServiceException("该时间段内房间已有锁定记录，存在冲突");
            }
        }
    }

    /**
     * 批量删除房间锁定管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否可以删除
            for (Long id : ids) {
                PmsRoomLock lock = baseMapper.selectById(id);
                if (lock != null && "active".equals(lock.getLockStatus())) {
                    throw new ServiceException("活跃状态的锁定记录不能删除，请先解锁");
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据房间ID查询活跃的锁定记录
     */
    @Override
    public List<PmsRoomLockVo> queryActiveLocksByRoomId(Long roomId) {
        return baseMapper.selectActiveLocksByRoomId(roomId);
    }

    /**
     * 根据部门ID查询锁定列表
     */
    @Override
    public List<PmsRoomLockVo> queryByDeptId(Long deptId) {
        return baseMapper.selectByDeptId(deptId);
    }

    /**
     * 根据锁定类型查询锁定列表
     */
    @Override
    public List<PmsRoomLockVo> queryByLockType(Long deptId, String lockType) {
        return baseMapper.selectByLockType(deptId, lockType);
    }

    /**
     * 查询指定时间范围内的锁定记录
     */
    @Override
    public List<PmsRoomLockVo> queryByTimeRange(Long deptId, Date startTime, Date endTime) {
        return baseMapper.selectByTimeRange(deptId, startTime, endTime);
    }

    /**
     * 检查房间在指定时间段是否有冲突的锁定
     */
    @Override
    public Boolean checkTimeConflict(Long roomId, Date startTime, Date endTime, Long excludeId) {
        Integer count = baseMapper.checkTimeConflict(roomId, startTime, endTime, excludeId);
        return count > 0;
    }

    /**
     * 解锁房间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlockRoom(Long lockId, String unlockReason) {
        PmsRoomLock lock = baseMapper.selectById(lockId);
        if (lock == null) {
            throw new ServiceException("锁定记录不存在");
        }
        if (!"active".equals(lock.getLockStatus())) {
            throw new ServiceException("该锁定记录已不是活跃状态");
        }

        PmsRoomLock update = new PmsRoomLock();
        update.setLockId(lockId);
        update.setLockStatus("unlocked");
        update.setUnlockTime(new Date());
        update.setUnlockReason(unlockReason);

        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量解锁房间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUnlockRooms(List<Long> lockIds, String unlockReason) {
        for (Long lockId : lockIds) {
            unlockRoom(lockId, unlockReason);
        }
        return true;
    }

    /**
     * 自动过期锁定记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean autoExpireLocks() {
        Date now = new Date();
        LambdaQueryWrapper<PmsRoomLock> lqw = Wrappers.lambdaQuery();
        lqw.eq(PmsRoomLock::getLockStatus, "active");
        lqw.isNotNull(PmsRoomLock::getLockEndTime);
        lqw.lt(PmsRoomLock::getLockEndTime, now);

        List<PmsRoomLock> expiredLocks = baseMapper.selectList(lqw);
        for (PmsRoomLock lock : expiredLocks) {
            PmsRoomLock update = new PmsRoomLock();
            update.setLockId(lock.getLockId());
            update.setLockStatus("expired");
            baseMapper.updateById(update);
        }

        return true;
    }
}
