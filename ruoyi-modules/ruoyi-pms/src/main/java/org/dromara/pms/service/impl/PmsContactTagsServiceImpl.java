package org.dromara.pms.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.dromara.pms.domain.bo.PmsContactTagsBo;
import org.dromara.pms.domain.vo.PmsContactTagsVo;
import org.dromara.pms.domain.PmsContactTags;
import org.dromara.pms.mapper.PmsContactTagsMapper;
import org.dromara.pms.mapper.PmsContactTagRelationsMapper;
import org.dromara.pms.service.IPmsContactTagsService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 联系人标签Service业务层处理
 *
 * @author xuhf
 * @date 2025-05-24
 */
@RequiredArgsConstructor
@Service
public class PmsContactTagsServiceImpl implements IPmsContactTagsService {

    private final PmsContactTagsMapper baseMapper;
    private final PmsContactTagRelationsMapper relationMapper;

    /**
     * 查询联系人标签
     *
     * @param tagId 主键
     * @return 联系人标签
     */
    @Override
    public PmsContactTagsVo queryById(Long tagId) {
        return baseMapper.selectVoById(tagId);
    }

    /**
     * 分页查询联系人标签列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 联系人标签分页列表
     */
    @Override
    public TableDataInfo<PmsContactTagsVo> queryPageList(PmsContactTagsBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsContactTags> lqw = buildQueryWrapper(bo);
        Page<PmsContactTagsVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的联系人标签列表
     *
     * @param bo 查询条件
     * @return 联系人标签列表
     */
    @Override
    public List<PmsContactTagsVo> queryList(PmsContactTagsBo bo) {
        LambdaQueryWrapper<PmsContactTags> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsContactTags> buildQueryWrapper(PmsContactTagsBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsContactTags> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, PmsContactTags::getDeptId, bo.getDeptId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), PmsContactTags::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getColor()), PmsContactTags::getColor, bo.getColor());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), PmsContactTags::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getDescription()), PmsContactTags::getDescription, bo.getDescription());
        lqw.eq(bo.getIsSystem() != null, PmsContactTags::getIsSystem, bo.getIsSystem());
        lqw.eq(bo.getSortOrder() != null, PmsContactTags::getSortOrder, bo.getSortOrder());
        lqw.orderByAsc(PmsContactTags::getSortOrder, PmsContactTags::getTagId);
        return lqw;
    }

    /**
     * 新增联系人标签
     *
     * @param bo 联系人标签
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsContactTagsBo bo) {
        PmsContactTags add = MapstructUtils.convert(bo, PmsContactTags.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setTagId(add.getTagId());
        }
        return flag;
    }

    /**
     * 修改联系人标签
     *
     * @param bo 联系人标签
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsContactTagsBo bo) {
        PmsContactTags update = MapstructUtils.convert(bo, PmsContactTags.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsContactTags entity) {
        // 检查标签名称在同一分类下的唯一性
        if (StringUtils.isNotBlank(entity.getName())) {
            LambdaQueryWrapper<PmsContactTags> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsContactTags::getName, entity.getName());
            wrapper.eq(entity.getCategory() != null, PmsContactTags::getCategory, entity.getCategory());
            wrapper.eq(entity.getDeptId() != null, PmsContactTags::getDeptId, entity.getDeptId());
            wrapper.ne(entity.getTagId() != null, PmsContactTags::getTagId, entity.getTagId());
            if (baseMapper.exists(wrapper)) {
                throw new RuntimeException("同一分类下标签名称已存在");
            }
        }
    }

    /**
     * 校验并批量删除联系人标签信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否有关联的联系人
            for (Long tagId : ids) {
                List<org.dromara.pms.domain.vo.PmsContactTagRelationsVo> relations = relationMapper
                        .selectRelationsByTagId(tagId);
                if (!relations.isEmpty()) {
                    throw new RuntimeException("标签已被联系人使用，无法删除");
                }
            }
        }

        // 删除标签时同时删除相关联系人关联
        for (Long tagId : ids) {
            relationMapper.deleteByTagId(tagId);
        }

        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据分类查询标签列表
     *
     * @param category 标签分类
     * @return 标签列表
     */
    @Override
    public List<PmsContactTagsVo> queryTagsByCategory(String category) {
        return baseMapper.selectTagsByCategory(category);
    }

    /**
     * 查询可用的标签分类列表
     *
     * @return 分类列表
     */
    @Override
    public List<String> queryDistinctCategories() {
        return baseMapper.selectDistinctCategories();
    }

    /**
     * 根据部门ID查询标签列表
     *
     * @param deptId 部门ID
     * @return 标签列表
     */
    @Override
    public List<PmsContactTagsVo> queryTagsByDeptId(Long deptId) {
        return baseMapper.selectTagsByDeptId(deptId);
    }

    /**
     * 查询所有可用标签（用于下拉选择）
     *
     * @return 标签列表
     */
    @Override
    public List<PmsContactTagsVo> queryAllAvailableTags() {
        LambdaQueryWrapper<PmsContactTags> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(PmsContactTags::getSortOrder, PmsContactTags::getTagId);
        return baseMapper.selectVoList(lqw);
    }
}
