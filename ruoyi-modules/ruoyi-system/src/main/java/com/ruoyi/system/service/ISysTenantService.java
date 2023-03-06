package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 租户Service接口
 *
 * @author Michelle.Chung
 */
public interface ISysTenantService {

    /**
     * 查询租户
     */
    SysTenantVo queryById(Long id);

    /**
     * 基于租户ID查询租户
     */
    SysTenantVo queryByTenantId(String tenantId);

    /**
     * 查询租户列表
     */
    TableDataInfo<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery);

    /**
     * 查询租户列表
     */
    List<SysTenantVo> queryList(SysTenantBo bo);

    /**
     * 新增租户
     */
    Boolean insertByBo(SysTenantBo bo);

    /**
     * 修改租户
     */
    Boolean updateByBo(SysTenantBo bo);

    /**
     * 修改租户状态
     */
    int updateTenantStatus(SysTenantBo bo);

    /**
     * 校验并批量删除租户信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验企业名称是否唯一
     */
    String checkCompanyNameUnique(SysTenantBo bo);

    /**
     * 校验账号余额
     */
    String checkAccountBalance(String tenantId);

    /**
     * 校验有效期
     */
    String checkExpireTime(String tenantId);

    /**
     * 同步租户套餐
     */
    Boolean syncTenantPackage(String tenantId, String packageId);
}
