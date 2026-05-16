package org.dromara.system.listener;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.system.event.OnlineUserCleanEvent;
import org.dromara.system.service.ISysRoleService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在线用户清理监听器。
 *
 * @author Lion Li
 */
@Component
@RequiredArgsConstructor
public class OnlineUserCleanListener {

    private final ISysRoleService roleService;

    /**
     * 权限或用户角色关系变化后清理受影响的在线用户。
     *
     * @param event 在线用户清理事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void cleanOnlineUser(OnlineUserCleanEvent event) {
        if (event.roleId() != null) {
            roleService.cleanOnlineUserByRole(event.roleId());
        }
        if (CollUtil.isNotEmpty(event.userIds())) {
            roleService.cleanOnlineUser(event.userIds());
        }
    }

}
