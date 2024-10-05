package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.Task;
import com.warm.flow.core.entity.User;
import com.warm.flow.orm.entity.FlowUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.domain.vo.ParticipantVo;

import java.util.*;

/**
 * 工作流工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowUtils {

    /**
     * 发送消息
     *
     * @param list        任务
     * @param name        流程名称
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    public static void sendMessage(List<Task> list, String name, List<String> messageType, String message, UserService userService) {
        Set<Long> userIds = new HashSet<>();
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + name + "】单据已经提交至您的待办，请您及时处理。";
        }
        for (Task t : list) {
            ParticipantVo taskParticipant = null; //= WorkflowUtils.getCurrentTaskParticipant(t.getId(), userService);
            if (CollUtil.isNotEmpty(taskParticipant.getGroupIds())) {
                List<Long> userIdList = userService.selectUserIdsByRoleIds(taskParticipant.getGroupIds());
                if (CollUtil.isNotEmpty(userIdList)) {
                    userIds.addAll(userIdList);
                }
            }
            List<Long> candidate = taskParticipant.getCandidate();
            if (CollUtil.isNotEmpty(candidate)) {
                userIds.addAll(candidate);
            }
        }
        if (CollUtil.isNotEmpty(userIds)) {
            List<UserDTO> userList = userService.selectListByIds(new ArrayList<>(userIds));
            for (String code : messageType) {
                MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
                if (ObjectUtil.isNotEmpty(messageTypeEnum)) {
                    switch (messageTypeEnum) {
                        case SYSTEM_MESSAGE:
                            WebSocketMessageDto dto = new WebSocketMessageDto();
                            dto.setSessionKeys(new ArrayList<>(userIds));
                            dto.setMessage(message);
                            WebSocketUtils.publishMessage(dto);
                            break;
                        case EMAIL_MESSAGE:
                            MailUtils.sendText(StreamUtils.join(userList, UserDTO::getEmail), "单据审批提醒", message);
                            break;
                        case SMS_MESSAGE:
                            //todo 短信发送
                            break;
                    }
                }
            }
        }
    }

    /**
     * 当前用户所有权限
     *
     * @return 权限列表
     */
    public static List<String> permissionList() {
        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        Long userId = LoginHelper.getUserId();
        Long deptId = LoginHelper.getDeptId();
        List<String> permissionList = StreamUtils.toList(roles, role -> "role:" + role.getRoleId());
        permissionList.add(String.valueOf(userId));
        permissionList.add("dept:" + deptId);
        return permissionList;
    }

    /**
     * 获取办理人
     *
     * @param userList 办理用户
     * @return 用户
     */
    public static List<UserDTO> getHandlerUser(List<User> userList) {
        List<UserDTO> userDTOList = new ArrayList<>();
        if (CollUtil.isNotEmpty(userList)) {
            UserService userService = SpringUtils.getBean(UserService.class);
            List<Long> userIds = new ArrayList<>();
            List<Long> roleIds = new ArrayList<>();
            List<Long> deptIds = new ArrayList<>();
            for (User user : userList) {
                if (user.getProcessedBy().startsWith("user:")) {
                    userIds.add(Long.valueOf(StringUtils.substringAfter(user.getProcessedBy(), StrUtil.C_COLON)));
                } else if (user.getProcessedBy().startsWith("role:")) {
                    roleIds.add(Long.valueOf(StringUtils.substringAfter(user.getProcessedBy(), StrUtil.C_COLON)));
                } else if (user.getProcessedBy().startsWith("dept:")) {
                    deptIds.add(Long.valueOf(StringUtils.substringAfter(user.getProcessedBy(), StrUtil.C_COLON)));
                } else {
                    userIds.add(Long.valueOf(user.getProcessedBy()));
                }
            }
            List<UserDTO> users = userService.selectListByIds(userIds);
            if (CollUtil.isNotEmpty(users)) {
                userDTOList.addAll(users);
            }
            List<UserDTO> roleUsers = userService.selectUsersByRoleIds(roleIds);
            if (CollUtil.isNotEmpty(roleUsers)) {
                userDTOList.addAll(roleUsers);
            }
            List<UserDTO> deptUsers = userService.selectUsersByDeptIds(deptIds);
            if (CollUtil.isNotEmpty(deptUsers)) {
                userDTOList.addAll(deptUsers);
            }
        }
        return userDTOList;
    }

    /**
     * 获取办理人
     *
     * @param userList 办理用户
     * @return 用户
     */
    public static Set<User> getUser(List<User> userList) {
        Set<User> list = new HashSet<>();
        if (CollUtil.isNotEmpty(userList)) {
            UserService userService = SpringUtils.getBean(UserService.class);
            for (User user : userList) {
                if (user.getProcessedBy().startsWith("user:")) {
                    Long userId = Long.valueOf(StringUtils.substringAfter(user.getProcessedBy(), StrUtil.C_COLON));
                    List<UserDTO> users = userService.selectListByIds(List.of(userId));
                    if (CollUtil.isNotEmpty(users)) {
                        FlowUser u = new FlowUser();
                        u.setType(user.getType());
                        u.setProcessedBy(String.valueOf(StreamUtils.toList(users, UserDTO::getUserId).get(0)));
                        list.add(u);
                    }
                }
                if (user.getProcessedBy().startsWith("role:")) {
                    Long roleId = Long.valueOf(StringUtils.substringAfter(user.getProcessedBy(), StrUtil.C_COLON));
                    List<UserDTO> roleUsers = userService.selectUsersByRoleIds(List.of(roleId));
                    for (UserDTO roleUser : roleUsers) {
                        FlowUser u = new FlowUser();
                        u.setType(user.getType());
                        u.setProcessedBy(String.valueOf(roleUser.getUserId()));
                        list.add(u);
                    }
                }
                if (user.getProcessedBy().startsWith("dept:")) {
                    Long deptId = Long.valueOf(StringUtils.substringAfter(user.getProcessedBy(), StrUtil.C_COLON));
                    List<UserDTO> deptUsers = userService.selectUsersByDeptIds(List.of(deptId));
                    for (UserDTO deptUser : deptUsers) {
                        FlowUser u = new FlowUser();
                        u.setType(user.getType());
                        u.setProcessedBy(String.valueOf(deptUser.getUserId()));
                        list.add(u);
                    }
                }
            }
        }
        return list;
    }
}
