package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsRoomLock;
import org.dromara.pms.domain.vo.PmsRoomLockVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Date;

/**
 * 房间锁定管理Mapper接口
 *
 * @author xuhf
 * @date 2025-05-28
 */
public interface PmsRoomLockMapper extends BaseMapperPlus<PmsRoomLock, PmsRoomLockVo> {

    /**
     * 根据锁定ID查询锁定详情（包含部门名称、房间号、操作人姓名）
     *
     * @param lockId 锁定ID
     * @return 锁定详情
     */
    PmsRoomLockVo selectVoByIdWithDetails(@Param("lockId") Long lockId);

    /**
     * 查询锁定列表（包含部门名称、房间号、操作人姓名）
     *
     * @param lock 锁定查询条件
     * @return 锁定列表
     */
    List<PmsRoomLockVo> selectVoListWithDetails(PmsRoomLock lock);

    /**
     * 根据房间ID查询活跃的锁定记录
     *
     * @param roomId 房间ID
     * @return 活跃的锁定记录列表
     */
    List<PmsRoomLockVo> selectActiveLocksByRoomId(@Param("roomId") Long roomId);

    /**
     * 根据部门ID查询锁定列表
     *
     * @param deptId 部门ID
     * @return 锁定列表
     */
    List<PmsRoomLockVo> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据锁定类型查询锁定列表
     *
     * @param deptId   部门ID
     * @param lockType 锁定类型
     * @return 锁定列表
     */
    List<PmsRoomLockVo> selectByLockType(@Param("deptId") Long deptId,
            @Param("lockType") String lockType);

    /**
     * 查询指定时间范围内的锁定记录
     *
     * @param deptId    部门ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 锁定列表
     */
    List<PmsRoomLockVo> selectByTimeRange(@Param("deptId") Long deptId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime);

    /**
     * 检查房间在指定时间段是否有冲突的锁定
     *
     * @param roomId    房间ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param excludeId 排除的锁定ID（编辑时使用）
     * @return 冲突的锁定记录数量
     */
    int checkTimeConflict(@Param("roomId") Long roomId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("excludeId") Long excludeId);

}
