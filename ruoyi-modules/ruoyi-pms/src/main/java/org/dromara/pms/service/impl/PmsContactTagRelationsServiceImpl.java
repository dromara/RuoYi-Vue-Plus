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
import org.dromara.pms.domain.bo.PmsContactTagRelationsBo;
import org.dromara.pms.domain.vo.PmsContactTagRelationsVo;
import org.dromara.pms.domain.PmsContactTagRelations;
import org.dromara.pms.mapper.PmsContactTagRelationsMapper;
import org.dromara.pms.service.IPmsContactTagRelationsService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;

/**
 * 联系人标签关联Service业务层处理
 *
 * @author xuhf
 * @date 2025-05-24
 */
@RequiredArgsConstructor
@Service
public class PmsContactTagRelationsServiceImpl implements IPmsContactTagRelationsService {

    private final PmsContactTagRelationsMapper baseMapper;

    /**
     * 查询联系人标签关联
     *
     * @param relationId 主键
     * @return 联系人标签关联
     */
    @Override
    public PmsContactTagRelationsVo queryById(Long relationId) {
        return baseMapper.selectVoById(relationId);
    }

    /**
     * 分页查询联系人标签关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 联系人标签关联分页列表
     */
    @Override
    public TableDataInfo<PmsContactTagRelationsVo> queryPageList(PmsContactTagRelationsBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsContactTagRelations> lqw = buildQueryWrapper(bo);
        Page<PmsContactTagRelationsVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的联系人标签关联列表
     *
     * @param bo 查询条件
     * @return 联系人标签关联列表
     */
    @Override
    public List<PmsContactTagRelationsVo> queryList(PmsContactTagRelationsBo bo) {
        LambdaQueryWrapper<PmsContactTagRelations> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsContactTagRelations> buildQueryWrapper(PmsContactTagRelationsBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsContactTagRelations> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getContactId() != null, PmsContactTagRelations::getContactId, bo.getContactId());
        lqw.eq(bo.getTagId() != null, PmsContactTagRelations::getTagId, bo.getTagId());
        lqw.orderByAsc(PmsContactTagRelations::getRelationId);
        return lqw;
    }

    /**
     * 新增联系人标签关联
     *
     * @param bo 联系人标签关联
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsContactTagRelationsBo bo) {
        PmsContactTagRelations add = MapstructUtils.convert(bo, PmsContactTagRelations.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setRelationId(add.getRelationId());
        }
        return flag;
    }

    /**
     * 修改联系人标签关联
     *
     * @param bo 联系人标签关联
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsContactTagRelationsBo bo) {
        PmsContactTagRelations update = MapstructUtils.convert(bo, PmsContactTagRelations.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsContactTagRelations entity) {
        // 检查联系人和标签的关联是否已存在
        if (entity.getContactId() != null && entity.getTagId() != null) {
            LambdaQueryWrapper<PmsContactTagRelations> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsContactTagRelations::getContactId, entity.getContactId());
            wrapper.eq(PmsContactTagRelations::getTagId, entity.getTagId());
            wrapper.ne(entity.getRelationId() != null, PmsContactTagRelations::getRelationId, entity.getRelationId());
            if (baseMapper.exists(wrapper)) {
                throw new RuntimeException("联系人和标签的关联已存在");
            }
        }
    }

    /**
     * 校验并批量删除联系人标签关联信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 可以在这里添加业务校验逻辑
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据联系人ID查询标签关联列表
     *
     * @param contactId 联系人ID
     * @return 关联列表
     */
    @Override
    public List<PmsContactTagRelationsVo> queryRelationsByContactId(Long contactId) {
        return baseMapper.selectRelationsByContactId(contactId);
    }

    /**
     * 根据标签ID查询关联的联系人列表
     *
     * @param tagId 标签ID
     * @return 关联列表
     */
    @Override
    public List<PmsContactTagRelationsVo> queryRelationsByTagId(Long tagId) {
        return baseMapper.selectRelationsByTagId(tagId);
    }

    /**
     * 批量保存联系人标签关联
     *
     * @param contactId 联系人ID
     * @param tagIds    标签ID列表
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSaveRelations(Long contactId, List<Long> tagIds) {
        if (contactId == null) {
            return false;
        }

        // 先删除该联系人的所有标签关联
        baseMapper.deleteByContactId(contactId);

        // 如果标签列表为空，则只删除不新增
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }

        // 批量插入新的关联
        List<PmsContactTagRelations> relations = new ArrayList<>();
        Date now = new Date();

        for (Long tagId : tagIds) {
            if (tagId != null) {
                PmsContactTagRelations relation = new PmsContactTagRelations();
                relation.setContactId(contactId);
                relation.setTagId(tagId);
                relation.setCreateTime(now);
                relations.add(relation);
            }
        }

        if (!relations.isEmpty()) {
            return baseMapper.batchInsertRelations(relations) > 0;
        }

        return true;
    }

    /**
     * 根据联系人ID删除所有标签关联
     *
     * @param contactId 联系人ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByContactId(Long contactId) {
        return baseMapper.deleteByContactId(contactId) >= 0;
    }

    /**
     * 根据标签ID删除所有关联
     *
     * @param tagId 标签ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByTagId(Long tagId) {
        return baseMapper.deleteByTagId(tagId) >= 0;
    }

    /**
     * 检查联系人和标签的关联是否存在
     *
     * @param contactId 联系人ID
     * @param tagId     标签ID
     * @return 是否存在
     */
    @Override
    public Boolean existsRelation(Long contactId, Long tagId) {
        return baseMapper.existsRelation(contactId, tagId);
    }
}
