package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsRoomRoom;
import org.dromara.pms.domain.vo.PmsRoomRoomVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 房间管理Mapper接口
 *
 * @author xuhf
 * @date 2025-05-28
 */
public interface PmsRoomRoomMapper extends BaseMapperPlus<PmsRoomRoom, PmsRoomRoomVo> {

    /**
     * 根据房间ID查询房间详情（包含部门名称和房型名称）
     *
     * @param roomId 房间ID
     * @return 房间详情
     */
    PmsRoomRoomVo selectVoByIdWithDetails(@Param("roomId") Long roomId);

    /**
     * 查询房间列表（包含部门名称和房型名称）
     *
     * @param room 房间查询条件
     * @return 房间列表
     */
    List<PmsRoomRoomVo> selectVoListWithDetails(PmsRoomRoom room);

    /**
     * 根据房间号查询房间（用于唯一性校验）
     *
     * @param roomNumber 房间号
     * @param deptId     部门ID
     * @param excludeId  排除的房间ID（编辑时使用）
     * @return 房间信息
     */
    PmsRoomRoom selectByRoomNumber(@Param("roomNumber") String roomNumber,
            @Param("deptId") Long deptId,
            @Param("excludeId") Long excludeId);

    /**
     * 根据房型ID查询房间列表
     *
     * @param roomTypeId 房型ID
     * @return 房间列表
     */
    List<PmsRoomRoomVo> selectByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    /**
     * 根据部门ID查询房间列表
     *
     * @param deptId 部门ID
     * @return 房间列表
     */
    List<PmsRoomRoomVo> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据房间状态查询房间列表
     *
     * @param deptId         部门ID
     * @param roomStatus     房间物理状态
     * @param cleaningStatus 清洁状态
     * @return 房间列表
     */
    List<PmsRoomRoomVo> selectByStatus(@Param("deptId") Long deptId,
            @Param("roomStatus") String roomStatus,
            @Param("cleaningStatus") String cleaningStatus);

}
