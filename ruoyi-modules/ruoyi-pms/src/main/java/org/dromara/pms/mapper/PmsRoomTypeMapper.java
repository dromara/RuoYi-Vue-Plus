package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsRoomType;
import org.dromara.pms.domain.vo.PmsRoomTypeVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 房型管理Mapper接口
 *
 * @author xuhf
 * @date 2025-05-28
 */
public interface PmsRoomTypeMapper extends BaseMapperPlus<PmsRoomType, PmsRoomTypeVo> {

    /**
     * 根据房型ID查询房型详情（包含部门名称）
     *
     * @param roomTypeId 房型ID
     * @return 房型详情
     */
    PmsRoomTypeVo selectVoByIdWithDept(@Param("roomTypeId") Long roomTypeId);

    /**
     * 查询房型列表（包含部门名称和房间数量统计）
     *
     * @param roomType 房型查询条件
     * @return 房型列表
     */
    List<PmsRoomTypeVo> selectVoListWithStats(PmsRoomType roomType);

    /**
     * 根据房型代码查询房型（用于唯一性校验）
     *
     * @param typeCode  房型代码
     * @param deptId    部门ID
     * @param excludeId 排除的房型ID（编辑时使用）
     * @return 房型信息
     */
    PmsRoomType selectByTypeCode(@Param("typeCode") String typeCode,
            @Param("deptId") Long deptId,
            @Param("excludeId") Long excludeId);

    /**
     * 根据部门ID查询房型列表
     *
     * @param deptId 部门ID
     * @return 房型列表
     */
    List<PmsRoomTypeVo> selectByDeptId(@Param("deptId") Long deptId);

}
