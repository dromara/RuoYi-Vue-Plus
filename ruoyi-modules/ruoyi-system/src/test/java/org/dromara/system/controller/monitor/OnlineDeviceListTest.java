package org.dromara.system.controller.monitor;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.ThreadUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.api.domain.UserOnlineDTO;
import org.dromara.system.domain.SysUserOnline;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnlineDeviceListTest {

    @org.junit.jupiter.api.Test
    void administratorCanRepeatLogoutOnAnExpiredSession() {
        try (var sessions = mockStatic(StpUtil.class)) {
            var expired = mock(cn.dev33.satoken.exception.NotLoginException.class);
            sessions.when(() -> StpUtil.kickoutByTokenValue("tablet")).thenAnswer(call -> null).thenThrow(expired);
            var controller = new SysUserOnlineController();
            assertTrue(org.dromara.common.core.domain.R.isSuccess(controller.forceLogout("tablet")));
            assertTrue(org.dromara.common.core.domain.R.isSuccess(controller.forceLogout("tablet")));
            sessions.verify(() -> StpUtil.kickoutByTokenValue("tablet"), times(2));
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"10.0.0.1,alice,phone","10.0.0.2,,tablet",
        ",alice,tablet|phone",",,tablet|phone","10.0.0.3,alice,''"})
    void administratorListAppliesBothFiltersAndDropsMissingEntries(String ip, String name, String expected) {
        var phone = new UserOnlineDTO();
        phone.setUserName("alice");
        phone.setIpaddr("10.0.0.1");
        phone.setTokenId("phone");
        var tablet = new UserOnlineDTO();
        tablet.setUserName("alice");
        tablet.setIpaddr("10.0.0.2");
        tablet.setTokenId("tablet");
        try (var beans = mockStatic(SpringUtil.class); var cache = mockStatic(RedisUtils.class);
             var threads = mockStatic(ThreadUtils.class)) {
            threads.when(() -> ThreadUtils.virtualSubmitAll(anyCollection())).thenAnswer(call ->
                new ArrayList<>(java.util.Arrays.asList(phone, null, tablet)));
            var result = new SysUserOnlineController().list(ip, name).getData();
            var tokens = result.getRows().stream().map(SysUserOnline::getTokenId).toList();
            assertEquals(expected, String.join("|", tokens));
            assertEquals(tokens.size(), result.getTotal());
        }
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void onlyListsActiveDevicesWithCachedDetails(boolean includeActive) {
        var original = StpUtil.stpLogic;
        var sessions = mock(StpLogic.class);
        try (var login = mockStatic(StpUtil.class); var beans = mockStatic(SpringUtil.class);
             var cache = mockStatic(RedisUtils.class); var threads = mockStatic(ThreadUtils.class)) {
            StpUtil.stpLogic = sessions;
            login.when(StpUtil::getLoginIdAsString).thenReturn("41");
            var tokens = includeActive ? List.of("expired", "phone", "missing", "tablet") : List.of("expired", "missing");
            login.when(() -> StpUtil.getTokenValueListByLoginId("41")).thenReturn(tokens);
            when(sessions.getTokenActiveTimeoutByToken("expired")).thenReturn(-2L);
            when(sessions.getTokenActiveTimeoutByToken("phone")).thenReturn(-1L);
            when(sessions.getTokenActiveTimeoutByToken("tablet")).thenReturn(60L);
            for (String token : List.of("phone", "tablet")) {
                var device = new UserOnlineDTO();
                device.setTokenId(token);
                cache.when(() -> RedisUtils.getCacheObject(CacheNames.ONLINE_TOKEN_KEY + token)).thenReturn(device);
            }
            // Execute the lookup tasks on the test thread where the Redis stub is installed.
            threads.when(() -> ThreadUtils.virtualSubmitAll(anyCollection())).thenAnswer(call -> {
                Collection<Supplier<UserOnlineDTO>> tasks = call.getArgument(0);
                var results = new ArrayList<UserOnlineDTO>();
                tasks.forEach(task -> results.add(task.get()));
                return results;
            });

            var page = new SysUserOnlineController().getInfo().getData();
            var expected = includeActive ? List.of("tablet", "phone") : List.of();
            assertEquals(expected, page.getRows().stream().map(SysUserOnline::getTokenId).toList());
            assertEquals(expected.size(), page.getTotal());
            cache.verify(() -> RedisUtils.getCacheObject(CacheNames.ONLINE_TOKEN_KEY + "expired"), never());
        } finally {
            StpUtil.stpLogic = original;
        }
    }
}
