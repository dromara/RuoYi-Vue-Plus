package org.dromara.workflow.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskAssigneeEnum;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.service.impl.TaskServiceImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * 审批前获取当前办理人，办理时会校验的该权限集合
     * 后续在{@link TaskServiceImpl#checkAuth(Task, FlowParams)} 中调用
     * 返回当前用户权限集合
     */
    @Override
    public List<String> permissions() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (ObjectUtil.isNull(loginUser)) {
            return new ArrayList<>();
        }
        // 使用一个流来构建权限列表
        return Stream.of(
                // 角色权限前缀
                loginUser.getRoles().stream()
                    .map(role -> TaskAssigneeEnum.ROLE.getCode() + role.getRoleId()),

                // 岗位权限前缀
                Stream.ofNullable(loginUser.getPosts())
                    .flatMap(Collection::stream)
                    .map(post -> TaskAssigneeEnum.POST.getCode() + post.getPostId()),

                // 用户和部门权限
                Stream.of(String.valueOf(loginUser.getUserId()),
                    TaskAssigneeEnum.DEPT.getCode() + loginUser.getDeptId()
                )
            )
            .flatMap(stream -> stream)
            .collect(Collectors.toList());
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

}
