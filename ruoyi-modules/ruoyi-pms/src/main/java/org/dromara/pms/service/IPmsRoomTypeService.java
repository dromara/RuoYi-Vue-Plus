package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsRoomTypeVo;
import org.dromara.pms.domain.bo.PmsRoomTypeBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 房型管理Service接口
 *
 * @author xuhf
 * @date 2025-05-28
 */
public interface IPmsRoomTypeService {

    /**
     * 查询房型管理
     *
     * @param roomTypeId 房型管理主键
     * @return 房型管理
     */
    PmsRoomTypeVo queryById(Long roomTypeId);

    /**
     * 查询房型管理列表
     *
     * @param bo        房型管理
     * @param pageQuery 分页查询
     * @return 房型管理集合
     */
    TableDataInfo<PmsRoomTypeVo> queryPageList(PmsRoomTypeBo bo, PageQuery pageQuery);

    /**
     * 查询房型管理列表
     *
     * @param bo 房型管理
     * @return 房型管理集合
     */
    List<PmsRoomTypeVo> queryList(PmsRoomTypeBo bo);

    /**
     * 新增房型管理
     *
     * @param bo 房型管理
     * @return 结果
     */
    Boolean insertByBo(PmsRoomTypeBo bo);

    /**
     * 修改房型管理
     *
     * @param bo 房型管理
     * @return 结果
     */
    Boolean updateByBo(PmsRoomTypeBo bo);

    /**
     * 校验并批量删除房型管理信息
     *
     * @param ids     需要删除的房型管理主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据部门ID查询房型列表
     *
     * @param deptId 部门ID
     * @return 房型列表
     */
    List<PmsRoomTypeVo> queryByDeptId(Long deptId);

    /**
     * 校验房型代码唯一性
     *
     * @param typeCode   房型代码
     * @param deptId     部门ID
     * @param roomTypeId 房型ID（编辑时排除自己）
     * @return 是否唯一
     */
    Boolean checkTypeCodeUnique(String typeCode, Long deptId, Long roomTypeId);

    /**
     * 获取房型选项列表（用于下拉选择）
     *
     * @param deptId 部门ID（可选）
     * @return 房型选项列表
     */
    List<Map<String, Object>> getOptions(Long deptId);

}
