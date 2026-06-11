package org.dromara.common.core.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.exception.ServiceException;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 时间工具类
 *
 * @author AprilWind
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils extends DateUtil {

    /**
     * 计算时间差并格式化（精确到秒）
     *
     * @param start 开始时间(支持 Date/LocalDateTime)
     * @param end   结束时间(支持 Date/LocalDateTime)
     * @return 时分秒格式时间差
     */
    public static String formatBetweenBySecond(Object start, Object end) {
        return formatTimeBetween(start, end, BetweenFormatter.Level.SECOND);
    }

    /**
     * 通用时间差格式化
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @param level     精度级别
     * @return 格式化时长
     */
    public static String formatTimeBetween(Object startDate, Object endDate, BetweenFormatter.Level level) {
        // 非空校验
        Assert.notNull(startDate, "开始时间不能为空");
        Assert.notNull(endDate, "结束时间不能为空");
        Assert.notNull(level, "时间精度级别不能为空");

        // 统一转为Date并校验格式合法性
        Date start = Convert.toDate(startDate);
        Date end = Convert.toDate(endDate);
        Assert.notNull(start, "开始时间格式错误，无法解析为时间");
        Assert.notNull(end, "结束时间格式错误，无法解析为时间");

        long diffMillis = Math.abs(end.getTime() - start.getTime());
        return formatBetween(diffMillis, level);
    }

    /**
     * 校验日期范围及最大时间跨度
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param maxValue  最大时间跨度限制值
     * @param unit      时间单位
     */
    public static void validateDateRange(Object startDate, Object endDate, int maxValue, TimeUnit unit) {
        // 基础非空校验
        Assert.notNull(startDate, "开始日期不能为空");
        Assert.notNull(endDate, "结束日期不能为空");
        Assert.notNull(unit, "时间单位不能为空");

        // 统一转换并校验时间合法性
        Date start = Convert.toDate(startDate);
        Date end = Convert.toDate(endDate);
        Assert.notNull(start, "开始日期格式错误，无法解析为时间");
        Assert.notNull(end, "结束日期格式错误，无法解析为时间");

        // 校验结束时间不能早于开始时间
        if (end.before(start)) {
            throw new ServiceException("结束日期不能早于开始日期");
        }

        long diffMillis = end.getTime() - start.getTime();
        long diff = switch (unit) {
            case DAYS -> TimeUnit.MILLISECONDS.toDays(diffMillis);
            case HOURS -> TimeUnit.MILLISECONDS.toHours(diffMillis);
            case MINUTES -> TimeUnit.MILLISECONDS.toMinutes(diffMillis);
            default -> throw new IllegalArgumentException("不支持的时间单位：" + unit.name());
        };

        if (diff > maxValue) {
            String msg = String.format("最大时间跨度为 %d %s", maxValue, unit.name().toLowerCase());
            throw new ServiceException(msg);
        }
    }

    /**
     * 根据指定日期时间获取时间段（凌晨 / 上午 / 中午 / 下午 / 晚上）
     *
     * @param date 日期时间
     * @return 时间段描述
     */
    public static String getTodayHour(Date date) {
        int hour = hour(date, true);
        if (hour <= 6) {
            return "凌晨";
        } else if (hour < 12) {
            return "上午";
        } else if (hour == 12) {
            return "中午";
        } else if (hour <= 18) {
            return "下午";
        } else {
            return "晚上";
        }
    }

    /**
     * 将日期格式化为仿微信的友好时间
     * <p>
     * 规则说明：
     * 1. 未来时间：yyyy-MM-dd HH:mm
     * 2. 今天：
     * - 1 分钟内：刚刚
     * - 1 小时内：X 分钟前
     * - 超过 1 小时：凌晨/上午/中午/下午/晚上 HH:mm
     * 3. 昨天：昨天 HH:mm
     * 4. 本周：周X HH:mm
     * 5. 今年内：MM-dd HH:mm
     * 6. 非今年：yyyy-MM-dd HH:mm
     *
     * @param date 日期时间
     * @return 格式化后的时间描述
     */
    public static String formatFriendlyTime(Date date) {
        if (date == null) {
            return "";
        }
        Date now = new Date();

        // 未来时间或非今年
        if (date.after(now) || year(date) != year(now)) {
            return formatDateTime(now);
        }

        // 今天
        if (isSameDay(date, now)) {
            long minutes = between(date, now, DateUnit.MINUTE);
            if (minutes < 1) {
                return "刚刚";
            }
            if (minutes < 60) {
                return minutes + "分钟前";
            }
            return getTodayHour(date) + " " + format(date, "HH:mm");
        }

        // 昨天
        if (isSameDay(date, yesterday())) {
            return "昨天 " + format(date, "HH:mm");
        }

        // 本周
        if (isSameWeek(date, now, true)) {
            return dayOfWeekEnum(date).toChinese("周") + " " + format(date, "HH:mm");
        }

        // 今年内其它时间
        return format(date, "MM-dd HH:mm");
    }

}
