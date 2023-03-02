package com.ruoyi.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.constant.CacheNames;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.constant.TenantConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.common.tenant.helper.TenantHelper;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 租户Service业务层处理
 *
 * @author Michelle.Chung
 */
@RequiredArgsConstructor
@Service
public class SysTenantServiceImpl implements ISysTenantService {

    private final SysTenantMapper baseMapper;
    private final SysTenantPackageMapper sysTenantPackageMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private final SysConfigMapper sysConfigMapper;

    /**
     * 查询租户
     */
    @Override
    public SysTenantVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 基于租户ID查询租户
     */
    @Cacheable(cacheNames = CacheNames.SYS_TENANT, key = "#tenantId")
    @Override
    public SysTenantVo queryByTenantId(String tenantId) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantId, tenantId));
    }

    /**
     * 查询租户列表
     */
    @Override
    public TableDataInfo<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTenant> lqw = buildQueryWrapper(bo);
        Page<SysTenantVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询租户列表
     */
    @Override
    public List<SysTenantVo> queryList(SysTenantBo bo) {
        LambdaQueryWrapper<SysTenant> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysTenant> buildQueryWrapper(SysTenantBo bo) {
        LambdaQueryWrapper<SysTenant> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), SysTenant::getTenantId, bo.getTenantId());
        lqw.like(StringUtils.isNotBlank(bo.getContactUserName()), SysTenant::getContactUserName, bo.getContactUserName());
        lqw.eq(StringUtils.isNotBlank(bo.getContactPhone()), SysTenant::getContactPhone, bo.getContactPhone());
        lqw.like(StringUtils.isNotBlank(bo.getCompanyName()), SysTenant::getCompanyName, bo.getCompanyName());
        lqw.eq(StringUtils.isNotBlank(bo.getLicenseNumber()), SysTenant::getLicenseNumber, bo.getLicenseNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getAddress()), SysTenant::getAddress, bo.getAddress());
        lqw.eq(StringUtils.isNotBlank(bo.getIntro()), SysTenant::getIntro, bo.getIntro());
        lqw.like(StringUtils.isNotBlank(bo.getDomain()), SysTenant::getDomain, bo.getDomain());
        lqw.eq(bo.getPackageId() != null, SysTenant::getPackageId, bo.getPackageId());
        lqw.eq(bo.getExpireTime() != null, SysTenant::getExpireTime, bo.getExpireTime());
        lqw.eq(bo.getAccountCount() != null, SysTenant::getAccountCount, bo.getAccountCount());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysTenant::getStatus, bo.getStatus());
        return lqw;
    }

    /**
     * 新增租户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTenantBo bo) {
        TenantHelper.enableIgnore();

        SysTenant add = MapstructUtils.convert(bo, SysTenant.class);

        // 获取所有租户编号
        List<String> tenantIds = baseMapper.selectObjs(
            new LambdaQueryWrapper<SysTenant>().select(SysTenant::getTenantId), Convert::toStr);
        String tenantId = generateTenantId(tenantIds);
        add.setTenantId(tenantId);
        boolean flag = baseMapper.insert(add) > 0;
        if (!flag) {
            TenantHelper.disableIgnore();
            throw new ServiceException("创建租户失败");
        }
        bo.setId(add.getId());

        // 根据套餐创建角色
        Long roleId = createTenantRole(tenantId, bo.getPackageId());

        // 创建部门: 公司名是部门名称
        SysDept dept = new SysDept();
        dept.setTenantId(tenantId);
        dept.setDeptName(bo.getCompanyName());
        dept.setLeader(bo.getUsername());
        dept.setParentId(Constants.TOP_PARENT_ID);
        dept.setAncestors(Constants.TOP_PARENT_ID.toString());
        sysDeptMapper.insert(dept);
        Long deptId = dept.getDeptId();

        // 角色和部门关联表
        SysRoleDept roleDept = new SysRoleDept();
        roleDept.setRoleId(roleId);
        roleDept.setDeptId(deptId);
        sysRoleDeptMapper.insert(roleDept);

        // 创建系统用户
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUserName(bo.getUsername());
        user.setNickName(bo.getUsername());
        user.setPassword(BCrypt.hashpw(bo.getPassword()));
        user.setDeptId(deptId);
        sysUserMapper.insert(user);

        // 用户和角色关联表
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(roleId);
        sysUserRoleMapper.insert(userRole);

        String defaultTenantId = TenantConstants.DEFAULT_TENANT_ID;
        List<SysDictType> dictTypeList = sysDictTypeMapper.selectList(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getTenantId, defaultTenantId));
        List<SysDictData> dictDataList = sysDictDataMapper.selectList(
            new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getTenantId, defaultTenantId));
        for (SysDictType dictType : dictTypeList) {
            dictType.setDictId(null);
            dictType.setTenantId(tenantId);
        }
        for (SysDictData dictData : dictDataList) {
            dictData.setDictCode(null);
            dictData.setTenantId(tenantId);
        }
        sysDictTypeMapper.insertBatch(dictTypeList);
        sysDictDataMapper.insertBatch(dictDataList);

        List<SysConfig> sysConfigList = sysConfigMapper.selectList(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getTenantId, defaultTenantId));
        for (SysConfig config : sysConfigList) {
            config.setConfigId(null);
            config.setTenantId(tenantId);
        }
        sysConfigMapper.insertBatch(sysConfigList);

        TenantHelper.disableIgnore();
        return true;
    }

    /**
     * 生成租户id
     *
     * @param tenantIds 已有租户id列表
     * @return 租户id
     */
    private String generateTenantId(List<String> tenantIds) {
        // 随机生成6位
        String numbers = RandomUtil.randomNumbers(6);
        // 判断是否存在，如果存在则重新生成
        if (tenantIds.contains(numbers)) {
            generateTenantId(tenantIds);
        }
        return numbers;
    }

    /**
     * 根据租户菜单创建租户角色
     *
     * @param tenantId  租户编号
     * @param packageId 租户套餐id
     * @return 角色id
     */
    public Long createTenantRole(String tenantId, Long packageId) {
        // 获取租户套餐
        SysTenantPackage tenantPackage = sysTenantPackageMapper.selectById(packageId);
        if (ObjectUtil.isNull(tenantPackage)) {
            throw new ServiceException("套餐不存在");
        }
        // 获取套餐菜单id
        List<Long> menuIds = StringUtils.splitTo(tenantPackage.getMenuIds(), Convert::toLong);

        // 创建角色
        SysRole role = new SysRole();
        role.setTenantId(tenantId);
        role.setRoleName(TenantConstants.TENANT_ADMIN_ROLE_NAME);
        role.setRoleKey(TenantConstants.TENANT_ADMIN_ROLE_KEY);
        role.setRoleSort(1);
        role.setStatus(TenantConstants.NORMAL);
        sysRoleMapper.insert(role);
        Long roleId = role.getRoleId();

        // 创建角色菜单
        List<SysRoleMenu> roleMenus = new ArrayList<>(menuIds.size());
        menuIds.forEach(menuId -> {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenus.add(roleMenu);
        });
        sysRoleMenuMapper.insertBatch(roleMenus);

        return roleId;
    }

    /**
     * 修改租户
     */
    @CacheEvict(cacheNames = CacheNames.SYS_TENANT, key = "#bo.tenantId")
    @Override
    public Boolean updateByBo(SysTenantBo bo) {
        SysTenant tenant = MapstructUtils.convert(bo, SysTenant.class);
        tenant.setTenantId(null);
        tenant.setPackageId(null);
        return baseMapper.updateById(tenant) > 0;
    }

    /**
     * 修改租户状态
     *
     * @param bo 租户信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_TENANT, key = "#bo.tenantId")
    @Override
    public int updateTenantStatus(SysTenantBo bo) {
        SysTenant tenant = MapstructUtils.convert(bo, SysTenant.class);
        return baseMapper.updateById(tenant);
    }

    /**
     * 批量删除租户
     */
    @CacheEvict(cacheNames = CacheNames.SYS_TENANT, allEntries = true)
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
            if (ids.contains(TenantConstants.SUPER_ADMIN_ID)) {
                throw new ServiceException("超管租户不能删除");
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验企业名称是否唯一
     */
    @Override
    public String checkCompanyNameUnique(SysTenantBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getCompanyName, bo.getCompanyName())
            .ne(ObjectUtil.isNotNull(bo.getTenantId()), SysTenant::getTenantId, bo.getTenantId()));
        if (exist) {
            return TenantConstants.NOT_PASS;
        }
        return TenantConstants.PASS;
    }

    /**
     * 校验账号余额
     */
    @Override
    public String checkAccountBalance(String tenantId) {
        SysTenantVo tenant = SpringUtils.getAopProxy(this).queryByTenantId(tenantId);
        // 如果余额为-1代表不限制
        if (tenant.getAccountCount() == -1) {
            return TenantConstants.PASS;
        }
        Long userNumber = sysUserMapper.selectCount(new LambdaQueryWrapper<>());
        // 如果余额大于0代表还有可用名额
        if (tenant.getAccountCount() - userNumber > 0) {
            return TenantConstants.PASS;
        }
        return TenantConstants.NOT_PASS;
    }

    /**
     * 校验有效期
     */
    @Override
    public String checkExpireTime(String tenantId) {
        SysTenantVo tenant = SpringUtils.getAopProxy(this).queryByTenantId(tenantId);
        // 如果未设置过期时间代表不限制
        if (ObjectUtil.isNull(tenant.getExpireTime())) {
            return TenantConstants.PASS;
        }
        // 如果当前时间在过期时间之前则通过
        if (new Date().before(tenant.getExpireTime())) {
            return TenantConstants.PASS;
        }
        return TenantConstants.NOT_PASS;
    }

}
