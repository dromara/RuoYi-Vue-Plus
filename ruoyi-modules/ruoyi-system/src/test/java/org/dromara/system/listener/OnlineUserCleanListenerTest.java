package org.dromara.system.listener;

import org.dromara.system.event.OnlineUserCleanEvent;
import org.dromara.system.service.ISysRoleService;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;

class OnlineUserCleanListenerTest {
    private final ISysRoleService roles = mock();
    private final OnlineUserCleanListener listener = new OnlineUserCleanListener(roles);

    @Test
    void emptyRecipientSnapshotStaysEmptyUntilDelivery() {
        var recipients = new ArrayList<Long>();
        var event = OnlineUserCleanEvent.byUsers(recipients);
        recipients.add(21L);
        listener.cleanOnlineUser(event);
        verifyNoInteractions(roles);
    }

    @Test
    void deliversRoleAndOriginalUsers() {
        var recipients = new ArrayList<>(List.of(21L));
        var event = new OnlineUserCleanEvent(7L, recipients);
        recipients.clear();
        listener.cleanOnlineUser(event);
        verify(roles).cleanOnlineUserByRole(7L);
        verify(roles).cleanOnlineUser(List.of(21L));
        verifyNoMoreInteractions(roles);
    }
}
