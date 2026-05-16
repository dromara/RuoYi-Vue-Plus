package org.dromara.system.event;

import cn.hutool.core.collection.CollUtil;

import java.util.Collection;
import java.util.List;

/**
 * 在线用户清理事件。
 *
 * @param roleId  角色 ID，存在时清理拥有该角色的在线用户
 * @param userIds 用户 ID 集合，存在时清理指定在线用户
 */
public record OnlineUserCleanEvent(Long roleId, Collection<Long> userIds) {

    public OnlineUserCleanEvent {
        if (CollUtil.isNotEmpty(userIds)) {
            userIds = List.copyOf(userIds);
        }
    }

    public static OnlineUserCleanEvent byRole(Long roleId) {
        return new OnlineUserCleanEvent(roleId, null);
    }

    public static OnlineUserCleanEvent byUsers(Collection<Long> userIds) {
        return new OnlineUserCleanEvent(null, userIds);
    }

}
