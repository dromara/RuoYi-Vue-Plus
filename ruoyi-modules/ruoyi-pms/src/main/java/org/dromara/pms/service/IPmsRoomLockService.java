package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsRoomLockVo;
import org.dromara.pms.domain.bo.PmsRoomLockBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Date;

/**
 * 房间锁定管理Service接口
 *
 * @author xuhf
 * @date 2025-05-28
 */
public interface IPmsRoomLockService {

    /**
     * 查询房间锁定管理
     */
    PmsRoomLockVo queryById(Long lockId);

    /**
     * 查询房间锁定管理列表
     */
    TableDataInfo<PmsRoomLockVo> queryPageList(PmsRoomLockBo bo, PageQuery pageQuery);

    /**
     * 查询房间锁定管理列表
     */
    List<PmsRoomLockVo> queryList(PmsRoomLockBo bo);

    /**
     * 新增房间锁定管理
     */
    Boolean insertByBo(PmsRoomLockBo bo);

    /**
     * 修改房间锁定管理
     */
    Boolean updateByBo(PmsRoomLockBo bo);

    /**
     * 校验并批量删除房间锁定管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据房间ID查询活跃的锁定记录
     */
    List<PmsRoomLockVo> queryActiveLocksByRoomId(Long roomId);

    /**
     * 根据部门ID查询锁定列表
     */
    List<PmsRoomLockVo> queryByDeptId(Long deptId);

    /**
     * 根据锁定类型查询锁定列表
     */
    List<PmsRoomLockVo> queryByLockType(Long deptId, String lockType);

    /**
     * 查询指定时间范围内的锁定记录
     */
    List<PmsRoomLockVo> queryByTimeRange(Long deptId, Date startTime, Date endTime);

    /**
     * 检查房间在指定时间段是否有冲突的锁定
     */
    Boolean checkTimeConflict(Long roomId, Date startTime, Date endTime, Long excludeId);

    /**
     * 解锁房间
     */
    Boolean unlockRoom(Long lockId, String unlockReason);

    /**
     * 批量解锁房间
     */
    Boolean batchUnlockRooms(List<Long> lockIds, String unlockReason);

    /**
     * 自动过期锁定记录
     */
    Boolean autoExpireLocks();

}
