package org.dromara.system.controller.monitor;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.domain.R;
import org.dromara.system.service.ISysLoginInfoService;
import org.dromara.system.service.ISysOperLogService;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemMonitoringControllerTest {

    @Test
    void reportsFailureWhenNoOperLogRowsWereDeleted() {
        var logs = mock(ISysOperLogService.class);
        Long[] ids = {11L, 14L};
        when(logs.deleteOperLogByIds(ids)).thenReturn(0);

        var response = new SysOperlogController(logs).remove(ids);

        assertTrue(R.isError(response));
        verify(logs).deleteOperLogByIds(ids);
    }

    @Test
    void reportsFailureWhenNoLoginInfoRowsWereDeleted() {
        var logs = mock(ISysLoginInfoService.class);
        Long[] ids = {11L, 14L};
        when(logs.deleteLoginInfoByIds(ids)).thenReturn(0);

        var response = new SysLoginInfoController(logs).remove(ids);

        assertTrue(R.isError(response));
        verify(logs).deleteLoginInfoByIds(ids);
    }

    @Test
    void onlyKicksDevicesOwnedByTheSignedInAccount() {
        var controller = new SysUserOnlineController();
        try (var sessions = mockStatic(StpUtil.class)) {
            sessions.when(StpUtil::getLoginIdAsString).thenReturn("21");
            sessions.when(() -> StpUtil.getTokenValueListByLoginId("21")).thenReturn(List.of("phone", "tablet"));

            controller.remove("other");
            controller.remove("tablet");

            sessions.verify(() -> StpUtil.kickoutByTokenValue("other"), never());
            sessions.verify(() -> StpUtil.kickoutByTokenValue("tablet"));
        }
    }

    @Test
    void reportsRedisCommandCallsAndDatabaseSize() throws Exception {
        var factory = mock(RedissonConnectionFactory.class);
        var connection = mock(RedisConnection.class, RETURNS_DEEP_STUBS);
        when(factory.getConnection()).thenReturn(connection);
        var stats = new Properties();
        stats.setProperty("cmdstat_get", "calls=4,usec=12,usec_per_call=3.00");
        when(connection.commands().info("commandstats")).thenReturn(stats);
        when(connection.commands().dbSize()).thenReturn(8L);

        var report = new CacheController(factory).getInfo().getData();

        assertEquals(8L, report.dbSize());
        assertEquals(List.of(Map.of("name", "get", "value", "4")), report.commandStats());
        verify(connection).close();
    }
}
