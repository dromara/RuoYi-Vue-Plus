package org.dromara.system.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.domain.bo.SysPostBo;
import org.dromara.system.domain.vo.SysPostVo;

import java.util.Collection;
import java.util.List;

/**
 * 岗位信息 服务层
 *
 * @author Lion Li
 */
public interface ISysPostService {

    /**
     * 分页查询岗位列表
     *
     * @param post      查询条件
     * @param pageQuery 分页参数
     * @return 岗位分页列表
     */
    PageResult<SysPostVo> selectPagePostList(SysPostBo post, PageQuery pageQuery);

    /**
     * 查询岗位信息集合
     *
     * @param post 岗位信息
     * @return 岗位列表
     */
    List<SysPostVo> selectPostList(SysPostBo post);

    /**
     * 查询用户所属岗位组
     *
     * @param userId 用户ID
     * @return 岗位列表
     */
    List<SysPostVo> selectPostsByUserId(Long userId);

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    List<SysPostVo> selectPostAll();

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 岗位信息
     */
    SysPostVo selectPostById(Long postId);

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    List<Long> selectPostListByUserId(Long userId);

    /**
     * 通过岗位ID串查询岗位
     *
     * @param postIds 岗位id串
     * @return 岗位列表信息
     */
    List<SysPostVo> selectPostByIds(Collection<Long> postIds);

    /**
     * 校验岗位名称
     *
     * @param post 岗位信息
     * @return 是否唯一
     */
    boolean checkPostNameUnique(SysPostBo post);

    /**
     * 校验岗位编码
     *
     * @param post 岗位信息
     * @return 是否唯一
     */
    boolean checkPostCodeUnique(SysPostBo post);

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 绑定用户数量
     */
    long countUserPostById(Long postId);

    /**
     * 通过部门ID查询岗位使用数量
     *
     * @param deptId 部门id
     * @return 岗位数量
     */
    long countPostByDeptId(Long deptId);

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 影响行数
     */
    int deletePostById(Long postId);

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 影响行数
     */
    int deletePostByIds(Collection<Long> postIds);

    /**
     * 新增保存岗位信息
     *
     * @param bo 岗位信息
     * @return 影响行数
     */
    int insertPost(SysPostBo bo);

    /**
     * 修改保存岗位信息
     *
     * @param bo 岗位信息
     * @return 影响行数
     */
    int updatePost(SysPostBo bo);
}
