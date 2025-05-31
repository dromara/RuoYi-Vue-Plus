package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsCustomerContacts;
import org.dromara.pms.domain.vo.PmsCustomerContactsVo;
import org.dromara.pms.domain.vo.PmsContactTagsVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import java.util.List;

/**
 * 客户联系人Mapper接口
 *
 * @author xuhf
 * @date 2025-05-24
 */
public interface PmsCustomerContactsMapper extends BaseMapperPlus<PmsCustomerContacts, PmsCustomerContactsVo> {

    /**
     * 查询联系人详情包含标签信息
     *
     * @param contactId 联系人ID
     * @return 联系人详情
     */
    PmsCustomerContactsVo selectVoByIdWithTags(Long contactId);

    /**
     * 查询联系人的标签列表
     *
     * @param contactId 联系人ID
     * @return 标签列表
     */
    List<PmsContactTagsVo> selectTagsByContactId(Long contactId);

}
