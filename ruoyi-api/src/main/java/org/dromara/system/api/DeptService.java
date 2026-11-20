package org.dromara.system.api;

import org.dromara.system.api.domain.DeptDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用 部门服务
 *
 * @author Lion Li
 */
public interface DeptService {

    /**
     * 通过部门ID查询部门名称
     *
     * @param deptIds 部门ID串逗号分隔
     * @return 部门名称串逗号分隔
     */
    String selectDeptNameByIds(String deptIds);

    /**
     * 根据部门ID查询部门负责人
     *
     * @param deptId 部门ID，用于指定需要查询的部门
     * @return 返回该部门的负责人ID
     */
    Long selectDeptLeaderById(Long deptId);

    /**
     * 查询部门
     *
     * @return 部门列表
     */
    List<DeptDTO> selectDeptsByList();

    /**
     * 根据部门 ID 列表查询部门名称映射关系
     *
     * @param deptIds 部门 ID 列表
     * @return Map，其中 key 为部门 ID，value 为对应的部门名称
     */
    Map<Long, String> selectDeptNamesByIds(Collection<Long> deptIds);

    /**
     * 根据ID查询部门
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    DeptDTO selectById(Long deptId);

    /**
     * 根据ID集合查询部门
     *
     * @param deptIds 部门ID 集合
     * @return 查询范围内的部门列表
     */
    List<DeptDTO> selectListByIds(Collection<Long> deptIds);

    /**
     * 查询所有层级的正常状态子部门ID，不包含自身
     *
     * @param deptId 部门ID
     * @return 数据权限范围内的正常状态子部门ID
     */
    List<Long> selectChildDeptIds(Long deptId);

    /**
     * 查询本部门及所有层级的正常状态子部门ID
     *
     * @param deptId 部门ID
     * @return 数据权限范围内的正常状态部门ID
     */
    List<Long> selectDeptAndChildIds(Long deptId);

}
