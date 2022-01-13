package com.ruoyi.framework.satoken.service;

import cn.dev33.satoken.stp.StpInterface;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.utils.LoginUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SaInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        UserType userType = LoginUtils.getUserType(loginId);
        if (userType == UserType.SYS_USER) {
            LoginUser loginUser = LoginUtils.getLoginUser();
            return new ArrayList<>(loginUser.getMenuPermission());
        } else if (userType == UserType.APP_USER) {
            // app端权限返回 自行根据业务编写
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserType userType = LoginUtils.getUserType(loginId);
        if (userType == UserType.SYS_USER) {
            LoginUser loginUser = LoginUtils.getLoginUser();
            return new ArrayList<>(loginUser.getRolePermission());
        } else if (userType == UserType.APP_USER) {
            // app端权限返回 自行根据业务编写
        }
        return new ArrayList<>();
    }
}
