package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsContactTagRelations;
import org.dromara.pms.domain.vo.PmsContactTagRelationsVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import java.util.List;

/**
 * 联系人标签关联Mapper接口
 *
 * @author xuhf
 * @date 2025-05-24
 */
public interface PmsContactTagRelationsMapper extends BaseMapperPlus<PmsContactTagRelations, PmsContactTagRelationsVo> {

    /**
     * 根据联系人ID查询标签关联列表
     *
     * @param contactId 联系人ID
     * @return 关联列表
     */
    List<PmsContactTagRelationsVo> selectRelationsByContactId(Long contactId);

    /**
     * 根据标签ID查询关联的联系人列表
     *
     * @param tagId 标签ID
     * @return 关联列表
     */
    List<PmsContactTagRelationsVo> selectRelationsByTagId(Long tagId);

    /**
     * 批量插入联系人标签关联
     *
     * @param relations 关联列表
     * @return 插入数量
     */
    int batchInsertRelations(List<PmsContactTagRelations> relations);

    /**
     * 根据联系人ID删除所有标签关联
     *
     * @param contactId 联系人ID
     * @return 删除数量
     */
    int deleteByContactId(Long contactId);

    /**
     * 根据标签ID删除所有关联
     *
     * @param tagId 标签ID
     * @return 删除数量
     */
    int deleteByTagId(Long tagId);

    /**
     * 检查联系人和标签的关联是否存在
     *
     * @param contactId 联系人ID
     * @param tagId     标签ID
     * @return 是否存在
     */
    boolean existsRelation(Long contactId, Long tagId);

}
