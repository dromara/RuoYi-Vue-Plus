package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsContactTags;
import org.dromara.pms.domain.vo.PmsContactTagsVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import java.util.List;

/**
 * 联系人标签Mapper接口
 *
 * @author xuhf
 * @date 2025-05-24
 */
public interface PmsContactTagsMapper extends BaseMapperPlus<PmsContactTags, PmsContactTagsVo> {

    /**
     * 根据分类查询标签列表
     *
     * @param category 标签分类
     * @return 标签列表
     */
    List<PmsContactTagsVo> selectTagsByCategory(String category);

    /**
     * 查询可用的标签分类列表
     *
     * @return 分类列表
     */
    List<String> selectDistinctCategories();

    /**
     * 根据部门ID查询标签列表
     *
     * @param deptId 部门ID
     * @return 标签列表
     */
    List<PmsContactTagsVo> selectTagsByDeptId(Long deptId);

}
