package org.dromara.common.satoken.core.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.PermissionService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * sa-token 权限管理实现类
 *
 * @author Lion Li
 */
public class SaPermissionImpl implements StpInterface {

    /**
     * 获取菜单权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (ObjectUtil.isNull(loginUser) || !loginUser.getLoginId().equals(loginId)) {
            PermissionService permissionService = getPermissionService();
            if (ObjectUtil.isNotNull(permissionService)) {
                List<String> list = StringUtils.splitList(loginId.toString(), ":");
                return new ArrayList<>(permissionService.getMenuPermission(Long.parseLong(list.get(1))));
            } else {
                throw new ServiceException("PermissionService 实现类不存在");
            }
        }
        UserType userType = UserType.getUserType(loginUser.getUserType());
        if (userType == UserType.APP_USER) {
            // 其他端 自行根据业务编写
        }
        // SYS_USER 默认返回权限
        return new ArrayList<>(loginUser.getMenuPermission());
    }

    /**
     * 获取角色权限列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (ObjectUtil.isNull(loginUser) || !loginUser.getLoginId().equals(loginId)) {
            PermissionService permissionService = getPermissionService();
            if (ObjectUtil.isNotNull(permissionService)) {
                List<String> list = StringUtils.splitList(loginId.toString(), ":");
                return new ArrayList<>(permissionService.getRolePermission(Long.parseLong(list.get(1))));
            } else {
                throw new ServiceException("PermissionService 实现类不存在");
            }
        }
        UserType userType = UserType.getUserType(loginUser.getUserType());
        if (userType == UserType.APP_USER) {
            // 其他端 自行根据业务编写
        }
        // SYS_USER 默认返回权限
        return new ArrayList<>(loginUser.getRolePermission());
    }

    private PermissionService getPermissionService() {
        try {
            return SpringUtils.getBean(PermissionService.class);
        } catch (Exception e) {
            return null;
        }
    }

}
