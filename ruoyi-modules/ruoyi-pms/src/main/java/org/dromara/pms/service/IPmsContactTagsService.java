package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsContactTagsVo;
import org.dromara.pms.domain.bo.PmsContactTagsBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 联系人标签Service接口
 *
 * @author xuhf
 * @date 2025-05-24
 */
public interface IPmsContactTagsService {

    /**
     * 查询联系人标签
     *
     * @param tagId 主键
     * @return 联系人标签
     */
    PmsContactTagsVo queryById(Long tagId);

    /**
     * 分页查询联系人标签列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 联系人标签分页列表
     */
    TableDataInfo<PmsContactTagsVo> queryPageList(PmsContactTagsBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的联系人标签列表
     *
     * @param bo 查询条件
     * @return 联系人标签列表
     */
    List<PmsContactTagsVo> queryList(PmsContactTagsBo bo);

    /**
     * 新增联系人标签
     *
     * @param bo 联系人标签
     * @return 是否新增成功
     */
    Boolean insertByBo(PmsContactTagsBo bo);

    /**
     * 修改联系人标签
     *
     * @param bo 联系人标签
     * @return 是否修改成功
     */
    Boolean updateByBo(PmsContactTagsBo bo);

    /**
     * 校验并批量删除联系人标签信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据分类查询标签列表
     *
     * @param category 标签分类
     * @return 标签列表
     */
    List<PmsContactTagsVo> queryTagsByCategory(String category);

    /**
     * 查询可用的标签分类列表
     *
     * @return 分类列表
     */
    List<String> queryDistinctCategories();

    /**
     * 根据部门ID查询标签列表
     *
     * @param deptId 部门ID
     * @return 标签列表
     */
    List<PmsContactTagsVo> queryTagsByDeptId(Long deptId);

    /**
     * 查询所有可用标签（用于下拉选择）
     *
     * @return 标签列表
     */
    List<PmsContactTagsVo> queryAllAvailableTags();
}
