package com.ruoyi.system.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.utils.LoginUtils;
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
        UserType userType = LoginUtils.getUserType(loginId);
        if (userType == UserType.SYS_USER) {
            Long userId = LoginUtils.getUserId();
            SysUser user = iSysUserService.getById(userId);
            Set<String> menuPermission = sysPermissionService.getMenuPermission(user);
            return new ArrayList<>(menuPermission);
        } else if (userType == UserType.APP_USER) {
            // app端权限返回 自行根据业务编写
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserType userType = LoginUtils.getUserType(loginId);
        if (userType == UserType.SYS_USER) {
            Long userId = LoginUtils.getUserId();
            SysUser user = iSysUserService.getById(userId);
            Set<String> rolePermission = sysPermissionService.getRolePermission(user);
            return new ArrayList<>(rolePermission);
        } else if (userType == UserType.APP_USER) {
            // app端权限返回 自行根据业务编写
        }
        return new ArrayList<>();
    }
}
