package org.dromara.pms.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.dromara.pms.domain.bo.PmsCustomerContactsBo;
import org.dromara.pms.domain.vo.PmsCustomerContactsVo;
import org.dromara.pms.domain.vo.PmsContactTagsVo;
import org.dromara.pms.domain.PmsCustomerContacts;
import org.dromara.pms.mapper.PmsCustomerContactsMapper;
import org.dromara.pms.service.IPmsCustomerContactsService;
import org.dromara.pms.service.IPmsContactTagRelationsService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 客户联系人Service业务层处理
 *
 * @author xuhf
 * @date 2025-05-24
 */
@RequiredArgsConstructor
@Service
public class PmsCustomerContactsServiceImpl implements IPmsCustomerContactsService {

    private final PmsCustomerContactsMapper baseMapper;
    private final IPmsContactTagRelationsService contactTagRelationsService;

    /**
     * 查询客户联系人
     *
     * @param contactId 主键
     * @return 客户联系人
     */
    @Override
    @Transactional(readOnly = true)
    public PmsCustomerContactsVo queryById(Long contactId) {
        PmsCustomerContactsVo vo = baseMapper.selectVoByIdWithTags(contactId);
        if (vo != null) {
            // 查询关联的标签信息
            List<PmsContactTagsVo> tags = baseMapper.selectTagsByContactId(contactId);
            vo.setTags(tags);

            // 设置标签ID列表，用于前端编辑时的数据绑定
            if (tags != null && !tags.isEmpty()) {
                List<Long> tagIds = tags.stream()
                        .map(PmsContactTagsVo::getTagId)
                        .collect(Collectors.toList());
                vo.setTagIds(tagIds);
            }
        }
        return vo;
    }

    /**
     * 分页查询客户联系人列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 客户联系人分页列表
     */
    @Override
    @Transactional(readOnly = true)
    public TableDataInfo<PmsCustomerContactsVo> queryPageList(PmsCustomerContactsBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsCustomerContacts> lqw = buildQueryWrapper(bo);
        Page<PmsCustomerContactsVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的客户联系人列表
     *
     * @param bo 查询条件
     * @return 客户联系人列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<PmsCustomerContactsVo> queryList(PmsCustomerContactsBo bo) {
        LambdaQueryWrapper<PmsCustomerContacts> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsCustomerContacts> buildQueryWrapper(PmsCustomerContactsBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsCustomerContacts> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFullName()), PmsCustomerContacts::getFullName, bo.getFullName());
        lqw.eq(StringUtils.isNotBlank(bo.getPhoneNumber()), PmsCustomerContacts::getPhoneNumber,
                bo.getPhoneNumber());
        lqw.like(StringUtils.isNotBlank(bo.getEmail()), PmsCustomerContacts::getEmail, bo.getEmail());
        lqw.eq(StringUtils.isNotBlank(bo.getWechatOpenid()), PmsCustomerContacts::getWechatOpenid,
                bo.getWechatOpenid());
        lqw.eq(StringUtils.isNotBlank(bo.getWechatUnionid()), PmsCustomerContacts::getWechatUnionid,
                bo.getWechatUnionid());
        lqw.eq(StringUtils.isNotBlank(bo.getGender()), PmsCustomerContacts::getGender, bo.getGender());
        lqw.eq(bo.getDateOfBirth() != null, PmsCustomerContacts::getDateOfBirth, bo.getDateOfBirth());
        lqw.eq(StringUtils.isNotBlank(bo.getIdType()), PmsCustomerContacts::getIdType, bo.getIdType());
        lqw.eq(StringUtils.isNotBlank(bo.getIdNumberEncrypted()), PmsCustomerContacts::getIdNumberEncrypted,
                bo.getIdNumberEncrypted());
        lqw.eq(StringUtils.isNotBlank(bo.getNationalityCountryCode()), PmsCustomerContacts::getNationalityCountryCode,
                bo.getNationalityCountryCode());
        lqw.eq(StringUtils.isNotBlank(bo.getAddressProvince()), PmsCustomerContacts::getAddressProvince,
                bo.getAddressProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getAddressCity()), PmsCustomerContacts::getAddressCity,
                bo.getAddressCity());
        lqw.eq(StringUtils.isNotBlank(bo.getAddressDistrict()), PmsCustomerContacts::getAddressDistrict,
                bo.getAddressDistrict());
        lqw.eq(StringUtils.isNotBlank(bo.getAddressDetail()), PmsCustomerContacts::getAddressDetail,
                bo.getAddressDetail());
        lqw.eq(StringUtils.isNotBlank(bo.getPostalCode()), PmsCustomerContacts::getPostalCode, bo.getPostalCode());
        lqw.eq(StringUtils.isNotBlank(bo.getContactStatus()), PmsCustomerContacts::getContactStatus,
                bo.getContactStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getMemberLevel()), PmsCustomerContacts::getMemberLevel, bo.getMemberLevel());
        lqw.eq(bo.getTotalStays() != null, PmsCustomerContacts::getTotalStays, bo.getTotalStays());
        lqw.eq(bo.getTotalAmount() != null, PmsCustomerContacts::getTotalAmount, bo.getTotalAmount());
        lqw.eq(bo.getLastStayDate() != null, PmsCustomerContacts::getLastStayDate, bo.getLastStayDate());
        lqw.eq(StringUtils.isNotBlank(bo.getRemarks()), PmsCustomerContacts::getRemarks, bo.getRemarks());
        return lqw;
    }

    /**
     * 新增客户联系人
     *
     * @param bo 客户联系人
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsCustomerContactsBo bo) {
        PmsCustomerContacts add = MapstructUtils.convert(bo, PmsCustomerContacts.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setContactId(add.getContactId());
        }
        return flag;
    }

    /**
     * 修改客户联系人
     *
     * @param bo 客户联系人
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsCustomerContactsBo bo) {
        PmsCustomerContacts update = MapstructUtils.convert(bo, PmsCustomerContacts.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsCustomerContacts entity) {
        // 检查手机号唯一性
        if (StringUtils.isNotBlank(entity.getPhoneNumber())) {
            LambdaQueryWrapper<PmsCustomerContacts> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsCustomerContacts::getPhoneNumber, entity.getPhoneNumber());
            wrapper.ne(entity.getContactId() != null, PmsCustomerContacts::getContactId, entity.getContactId());
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("手机号已存在");
            }
        }

        // 检查邮箱唯一性
        if (StringUtils.isNotBlank(entity.getEmail())) {
            LambdaQueryWrapper<PmsCustomerContacts> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsCustomerContacts::getEmail, entity.getEmail());
            wrapper.ne(entity.getContactId() != null, PmsCustomerContacts::getContactId, entity.getContactId());
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("邮箱已存在");
            }
        }

        // 检查微信OpenID唯一性
        if (StringUtils.isNotBlank(entity.getWechatOpenid())) {
            LambdaQueryWrapper<PmsCustomerContacts> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsCustomerContacts::getWechatOpenid, entity.getWechatOpenid());
            wrapper.ne(entity.getContactId() != null, PmsCustomerContacts::getContactId, entity.getContactId());
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("微信OpenID已存在");
            }
        }
    }

    /**
     * 校验并批量删除客户联系人信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否有关联的业务数据
            for (Long id : ids) {
                PmsCustomerContactsVo contact = baseMapper.selectVoById(id);
                if (contact != null) {
                    // 可以在这里添加其他业务校验逻辑
                    // 例如：检查是否有关联的订单等
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 保存联系人标签关联
     *
     * @param contactId 联系人ID
     * @param tagIds    标签ID列表
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveContactTags(Long contactId, List<Long> tagIds) {
        return contactTagRelationsService.batchSaveRelations(contactId, tagIds);
    }

    /**
     * 查询联系人详情包含标签信息
     *
     * @param contactId 联系人ID
     * @return 联系人详情
     */
    @Override
    @Transactional(readOnly = true)
    public PmsCustomerContactsVo queryByIdWithTags(Long contactId) {
        return queryById(contactId);
    }
}
