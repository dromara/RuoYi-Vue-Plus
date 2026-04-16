package org.dromara.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.service.IFlwTaskAssigneeService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 办理人权限处理器
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Component
@Slf4j
public class WorkflowPermissionHandler implements PermissionHandler {

    private final IFlwTaskAssigneeService flwTaskAssigneeService;

    /**
     * 办理人权限标识，比如用户，角色，部门等，用于校验是否有权限办理任务
     * 后续在{@link FlowParams#getPermissionFlag}  中获取
     * 返回当前用户权限集合
     *
     * @return 当前用户权限集合
     */
    @Override
    public List<String> permissions() {
        return Collections.singletonList(LoginHelper.getUserIdStr());
    }

    /**
     * 获取当前办理人
     *
     * @return 当前办理人
     */
    @Override
    public String getHandler() {
        return LoginHelper.getUserIdStr();
    }

    /**
     * 将预设办理人转换为实际用户 ID 列表。
     *
     * @param permissions 预设权限标识列表
     * @return 用户 ID 列表
     */
    @Override
    public List<String> convertPermissions(List<String> permissions) {
        if (CollUtil.isEmpty(permissions)) {
            return permissions;
        }
        String storageIds = CollUtil.join(permissions, StringUtils.SEPARATOR);
        List<UserDTO> users = flwTaskAssigneeService.fetchUsersByStorageIds(storageIds);
        return StreamUtils.toList(users, userDTO -> Convert.toStr(userDTO.getUserId()));
    }
}
