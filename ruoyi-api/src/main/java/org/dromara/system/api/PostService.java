package org.dromara.system.api;

import org.dromara.system.api.domain.PostDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用 岗位服务
 *
 * @author AprilWind
 */
public interface PostService {

    /**
     * 根据岗位 ID 列表查询岗位名称映射关系
     *
     * @param postIds 岗位 ID 列表
     * @return Map，其中 key 为岗位 ID，value 为对应的岗位名称
     */
    Map<Long, String> selectPostNamesByIds(Collection<Long> postIds);

    /**
     * 根据ID查询岗位
     *
     * @param postId 岗位ID
     * @return 岗位信息
     */
    PostDTO selectById(Long postId);

    /**
     * 根据ID集合查询岗位
     *
     * @param postIds 岗位ID 集合
     * @return 查询范围内的岗位列表
     */
    List<PostDTO> selectListByIds(Collection<Long> postIds);

    /**
     * 查询用户关联的岗位
     *
     * @param userId 用户ID
     * @return 用户关联的岗位列表
     */
    List<PostDTO> selectListByUserId(Long userId);

    /**
     * 根据部门ID查询岗位列表
     *
     * @param deptId 部门ID
     * @return 指定部门的岗位列表
     */
    List<PostDTO> selectListByDeptId(Long deptId);

}
