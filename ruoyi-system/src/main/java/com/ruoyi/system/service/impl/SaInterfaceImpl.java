package com.ruoyi.system.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SaInterfaceImpl implements StpInterface {

    @Autowired
    private SysPermissionService sysPermissionService;
    @Autowired
    private ISysUserService iSysUserService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SysUser user = iSysUserService.getById(loginId.toString());
        Set<String> menuPermission = sysPermissionService.getMenuPermission(user);
        //采用的是用户里自带的权限，实现一次性访问reids,进行判断是否可以访问
        return new ArrayList<>(menuPermission);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = iSysUserService.getById(loginId.toString());
        Set<String> rolePermission = sysPermissionService.getRolePermission(user);
        return new ArrayList<>(rolePermission);
    }
}
