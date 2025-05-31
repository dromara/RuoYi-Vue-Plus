package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsContactTagRelationsVo;
import org.dromara.pms.domain.bo.PmsContactTagRelationsBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 联系人标签关联Service接口
 *
 * @author xuhf
 * @date 2025-05-24
 */
public interface IPmsContactTagRelationsService {

    /**
     * 查询联系人标签关联
     *
     * @param relationId 主键
     * @return 联系人标签关联
     */
    PmsContactTagRelationsVo queryById(Long relationId);

    /**
     * 分页查询联系人标签关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 联系人标签关联分页列表
     */
    TableDataInfo<PmsContactTagRelationsVo> queryPageList(PmsContactTagRelationsBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的联系人标签关联列表
     *
     * @param bo 查询条件
     * @return 联系人标签关联列表
     */
    List<PmsContactTagRelationsVo> queryList(PmsContactTagRelationsBo bo);

    /**
     * 新增联系人标签关联
     *
     * @param bo 联系人标签关联
     * @return 是否新增成功
     */
    Boolean insertByBo(PmsContactTagRelationsBo bo);

    /**
     * 修改联系人标签关联
     *
     * @param bo 联系人标签关联
     * @return 是否修改成功
     */
    Boolean updateByBo(PmsContactTagRelationsBo bo);

    /**
     * 校验并批量删除联系人标签关联信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据联系人ID查询标签关联列表
     *
     * @param contactId 联系人ID
     * @return 关联列表
     */
    List<PmsContactTagRelationsVo> queryRelationsByContactId(Long contactId);

    /**
     * 根据标签ID查询关联的联系人列表
     *
     * @param tagId 标签ID
     * @return 关联列表
     */
    List<PmsContactTagRelationsVo> queryRelationsByTagId(Long tagId);

    /**
     * 批量保存联系人标签关联
     *
     * @param contactId 联系人ID
     * @param tagIds    标签ID列表
     * @return 是否保存成功
     */
    Boolean batchSaveRelations(Long contactId, List<Long> tagIds);

    /**
     * 根据联系人ID删除所有标签关联
     *
     * @param contactId 联系人ID
     * @return 是否删除成功
     */
    Boolean deleteByContactId(Long contactId);

    /**
     * 根据标签ID删除所有关联
     *
     * @param tagId 标签ID
     * @return 是否删除成功
     */
    Boolean deleteByTagId(Long tagId);

    /**
     * 检查联系人和标签的关联是否存在
     *
     * @param contactId 联系人ID
     * @param tagId     标签ID
     * @return 是否存在
     */
    Boolean existsRelation(Long contactId, Long tagId);
}
