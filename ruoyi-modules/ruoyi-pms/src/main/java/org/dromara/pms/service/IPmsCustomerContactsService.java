package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsCustomerContactsVo;
import org.dromara.pms.domain.bo.PmsCustomerContactsBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 客户联系人Service接口
 *
 * @author xuhf
 * @date 2025-05-24
 */
public interface IPmsCustomerContactsService {

    /**
     * 查询客户联系人
     *
     * @param contactId 主键
     * @return 客户联系人
     */
    PmsCustomerContactsVo queryById(Long contactId);

    /**
     * 分页查询客户联系人列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 客户联系人分页列表
     */
    TableDataInfo<PmsCustomerContactsVo> queryPageList(PmsCustomerContactsBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的客户联系人列表
     *
     * @param bo 查询条件
     * @return 客户联系人列表
     */
    List<PmsCustomerContactsVo> queryList(PmsCustomerContactsBo bo);

    /**
     * 新增客户联系人
     *
     * @param bo 客户联系人
     * @return 是否新增成功
     */
    Boolean insertByBo(PmsCustomerContactsBo bo);

    /**
     * 修改客户联系人
     *
     * @param bo 客户联系人
     * @return 是否修改成功
     */
    Boolean updateByBo(PmsCustomerContactsBo bo);

    /**
     * 校验并批量删除客户联系人信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 保存联系人标签关联
     *
     * @param contactId 联系人ID
     * @param tagIds    标签ID列表
     * @return 是否保存成功
     */
    Boolean saveContactTags(Long contactId, List<Long> tagIds);

    /**
     * 查询联系人详情包含标签信息
     *
     * @param contactId 联系人ID
     * @return 联系人详情
     */
    PmsCustomerContactsVo queryByIdWithTags(Long contactId);
}
