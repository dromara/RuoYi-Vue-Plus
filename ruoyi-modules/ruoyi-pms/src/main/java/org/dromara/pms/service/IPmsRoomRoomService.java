package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsRoomRoomVo;
import org.dromara.pms.domain.bo.PmsRoomRoomBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 房间管理Service接口
 *
 * @author xuhf
 * @date 2025-05-28
 */
public interface IPmsRoomRoomService {

    /**
     * 查询房间管理
     */
    PmsRoomRoomVo queryById(Long roomId);

    /**
     * 查询房间管理列表
     */
    TableDataInfo<PmsRoomRoomVo> queryPageList(PmsRoomRoomBo bo, PageQuery pageQuery);

    /**
     * 查询房间管理列表
     */
    List<PmsRoomRoomVo> queryList(PmsRoomRoomBo bo);

    /**
     * 新增房间管理
     */
    Boolean insertByBo(PmsRoomRoomBo bo);

    /**
     * 修改房间管理
     */
    Boolean updateByBo(PmsRoomRoomBo bo);

    /**
     * 校验并批量删除房间管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据房型ID查询房间列表
     */
    List<PmsRoomRoomVo> queryByRoomTypeId(Long roomTypeId);

    /**
     * 根据部门ID查询房间列表
     */
    List<PmsRoomRoomVo> queryByDeptId(Long deptId);

    /**
     * 根据房间状态查询房间列表
     */
    List<PmsRoomRoomVo> queryByStatus(Long deptId, String roomStatus, String cleaningStatus);

    /**
     * 更新房间状态
     */
    Boolean updateRoomStatus(Long roomId, String roomStatus, String statusRemarks);

    /**
     * 更新清洁状态
     */
    Boolean updateCleaningStatus(Long roomId, String cleaningStatus);

    /**
     * 批量更新房间状态
     */
    Boolean batchUpdateRoomStatus(List<Long> roomIds, String roomStatus, String statusRemarks);

    /**
     * 批量更新清洁状态
     */
    Boolean batchUpdateCleaningStatus(List<Long> roomIds, String cleaningStatus);

}
