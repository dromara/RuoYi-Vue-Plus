package org.dromara.common.core.utils;

import cn.hutool.core.date.DateUtil;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("日期、网络与脱敏工具单元测试")
class DateNetDesensitizedUtilsTest {

    /**
     * 验证时间差可以忽略参数顺序并按秒精度格式化。
     */
    @Test
    @DisplayName("格式化时间差")
    void shouldFormatTimeDifferenceBySecond() {
        Date start = new Date(0);
        Date end = new Date(3_661_000);

        assertEquals("1小时1分1秒", DateUtils.formatBetweenBySecond(start, end));
        assertEquals("1小时1分1秒", DateUtils.formatBetweenBySecond(end, start));
    }

    /**
     * 验证日期范围接受边界值，并拒绝倒序、超限和不支持的单位。
     */
    @Test
    @DisplayName("校验日期范围")
    void shouldValidateDateRangeAndUnits() {
        Date start = DateUtil.parse("2026-01-01 00:00:00");
        Date end = DateUtil.parse("2026-01-03 00:00:00");

        assertDoesNotThrow(() -> DateUtils.validateDateRange(start, end, 2, TimeUnit.DAYS));
        assertThrows(ServiceException.class, () -> DateUtils.validateDateRange(end, start, 2, TimeUnit.DAYS));
        assertThrows(ServiceException.class, () -> DateUtils.validateDateRange(start, end, 1, TimeUnit.DAYS));
        assertThrows(IllegalArgumentException.class,
            () -> DateUtils.validateDateRange(start, end, 1, TimeUnit.SECONDS));
    }

    /**
     * 验证一天内各小时会映射到凌晨、上午、中午、下午和晚上。
     */
    @Test
    @DisplayName("识别当天时间段")
    void shouldResolveTodayPeriod() {
        assertEquals("凌晨", DateUtils.getTodayHour(DateUtil.parse("2026-01-01 06:00:00")));
        assertEquals("上午", DateUtils.getTodayHour(DateUtil.parse("2026-01-01 09:00:00")));
        assertEquals("中午", DateUtils.getTodayHour(DateUtil.parse("2026-01-01 12:00:00")));
        assertEquals("下午", DateUtils.getTodayHour(DateUtil.parse("2026-01-01 15:00:00")));
        assertEquals("晚上", DateUtils.getTodayHour(DateUtil.parse("2026-01-01 20:00:00")));
    }

    /**
     * 验证友好时间处理空值、刚刚、分钟前以及未来时间，并确保未来时间使用目标日期。
     */
    @Test
    @DisplayName("格式化友好时间")
    void shouldFormatFriendlyTimeUsingTargetDate() {
        Date now = new Date();
        Date future = DateUtil.offsetDay(now, 2);

        assertEquals("", DateUtils.formatFriendlyTime(null));
        assertEquals("刚刚", DateUtils.formatFriendlyTime(DateUtil.offsetSecond(now, -10)));
        assertTrue(DateUtils.formatFriendlyTime(DateUtil.offsetMinute(now, -5)).endsWith("分钟前"));
        assertEquals(DateUtils.formatDateTime(future), DateUtils.formatFriendlyTime(future));
    }

    /**
     * 验证 IPv4、IPv6、精确地址、通配符和 CIDR 规则的匹配结果。
     */
    @Test
    @DisplayName("匹配 IP 地址规则")
    void shouldMatchIpAddressRules() {
        assertTrue(NetUtils.isIPv4("192.168.1.1"));
        assertFalse(NetUtils.isIPv4("999.1.1.1"));
        assertTrue(NetUtils.isIPv6("::1"));
        assertTrue(NetUtils.isInnerIPv6("::1"));
        assertTrue(NetUtils.isMatchIpRule("192.168.1.10", "192.168.1.10"));
        assertTrue(NetUtils.isMatchIpRule("192.168.*.?", "192.168.1.8"));
        assertTrue(NetUtils.isMatchIpRule("10.0.0.0/8", "10.20.30.40"));
        assertFalse(NetUtils.isMatchIpRule("10.0.0.0/8", "11.20.30.40"));
        assertFalse(NetUtils.isMatchCidr("10.0.0.0/99", "10.0.0.1"));
        assertFalse(NetUtils.isMatchIpRule(" ", "10.0.0.1"));
    }

    /**
     * 验证普通脱敏在短值、临界值和标准长度下应用固定掩码规则。
     */
    @Test
    @DisplayName("应用固定长度脱敏")
    void shouldMaskValuesWithFixedLength() {
        assertNull(DesensitizedUtils.mask(null, 2, 2, 4));
        assertEquals("***", DesensitizedUtils.mask("abc", 2, 2, 4));
        assertEquals("ab****", DesensitizedUtils.mask("abcdef", 2, 2, 4));
        assertEquals("ab****g", DesensitizedUtils.mask("abcdefg", 2, 2, 4));
        assertEquals("ab****ij", DesensitizedUtils.mask("abcdefghij", 2, 2, 4));
    }

    /**
     * 验证高安全脱敏对短 Token 全掩码，并在长 Token 中仅保留指定首尾字符。
     */
    @Test
    @DisplayName("应用高安全脱敏")
    void shouldMaskHighSecurityValues() {
        assertEquals("***", DesensitizedUtils.maskHighSecurity("abc", 3, 2));
        assertEquals("ab***", DesensitizedUtils.maskHighSecurity("abcde", 3, 2));
        assertEquals("ab******ij", DesensitizedUtils.maskHighSecurity("abcdefghij", 2, 2));
    }
}
