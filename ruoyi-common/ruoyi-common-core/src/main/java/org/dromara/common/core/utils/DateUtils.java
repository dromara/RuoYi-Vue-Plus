package org.dromara.common.core.utils;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.dromara.common.core.enums.FormatsType;
import org.dromara.common.core.exception.ServiceException;

import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 时间工具类
 *
 * @author ruoyi
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    private static final String[] PARSE_PATTERNS = {
        "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
        "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
        "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    @Deprecated
    private DateUtils() {
    }

    /**
     * 获取当前日期和时间
     *
     * @return 当前日期和时间的 Date 对象表示
     */
    public static Date getNowDate() {
        return new Date();
    }

    /**
     * 获取当前日期的字符串表示，格式为YYYY-MM-DD
     *
     * @return 当前日期的字符串表示
     */
    public static String getDate() {
        return dateTimeNow(FormatsType.YYYY_MM_DD);
    }

    /**
     * 获取当前日期的字符串表示，格式为yyyyMMdd
     *
     * @return 当前日期的字符串表示
     */
    public static String getCurrentDate() {
        return DateFormatUtils.format(new Date(), FormatsType.YYYYMMDD.getTimeFormat());
    }

    /**
     * 获取当前日期的路径格式字符串，格式为"yyyy/MM/dd"
     *
     * @return 当前日期的路径格式字符串
     */
    public static String datePath() {
        Date now = new Date();
        return DateFormatUtils.format(now, FormatsType.YYYY_MM_DD_SLASH.getTimeFormat());
    }

    /**
     * 获取当前时间的字符串表示，格式为YYYY-MM-DD HH:MM:SS
     *
     * @return 当前时间的字符串表示
     */
    public static String getTime() {
        return dateTimeNow(FormatsType.YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 获取当前时间的字符串表示，格式为 "HH:MM:SS"
     *
     * @return 当前时间的字符串表示，格式为 "HH:MM:SS"
     */
    public static String getTimeWithHourMinuteSecond() {
        return dateTimeNow(FormatsType.HH_MM_SS);
    }

    /**
     * 获取当前日期和时间的字符串表示，格式为YYYYMMDDHHMMSS
     *
     * @return 当前日期和时间的字符串表示
     */
    public static String dateTimeNow() {
        return dateTimeNow(FormatsType.YYYYMMDDHHMMSS);
    }

    /**
     * 获取当前日期和时间的指定格式的字符串表示
     *
     * @param format 日期时间格式，例如"YYYY-MM-DD HH:MM:SS"
     * @return 当前日期和时间的字符串表示
     */
    public static String dateTimeNow(final FormatsType format) {
        return parseDateToStr(format, new Date());
    }

    /**
     * 将指定日期格式化为 YYYY-MM-DD 格式的字符串
     *
     * @param date 要格式化的日期对象
     * @return 格式化后的日期字符串
     */
    public static String formatDate(final Date date) {
        return parseDateToStr(FormatsType.YYYY_MM_DD, date);
    }

    /**
     * 将指定日期格式化为 YYYY-MM-DD HH:MM:SS 格式的字符串
     *
     * @param date 要格式化的日期对象
     * @return 格式化后的日期时间字符串
     */
    public static String formatDateTime(final Date date) {
        return parseDateToStr(FormatsType.YYYY_MM_DD_HH_MM_SS, date);
    }

    /**
     * 将指定日期按照指定格式进行格式化
     *
     * @param format 要使用的日期时间格式，例如"YYYY-MM-DD HH:MM:SS"
     * @param date   要格式化的日期对象
     * @return 格式化后的日期时间字符串
     */
    public static String parseDateToStr(final FormatsType format, final Date date) {
        return new SimpleDateFormat(format.getTimeFormat()).format(date);
    }

    /**
     * 将指定格式的日期时间字符串转换为 Date 对象
     *
     * @param format 要解析的日期时间格式，例如"YYYY-MM-DD HH:MM:SS"
     * @param ts     要解析的日期时间字符串
     * @return 解析后的 Date 对象
     * @throws RuntimeException 如果解析过程中发生异常
     */
    public static Date parseDateTime(final FormatsType format, final String ts) {
        try {
            return new SimpleDateFormat(format.getTimeFormat()).parse(ts);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转换为日期对象
     *
     * @param str 要转换的对象，通常是字符串
     * @return 转换后的日期对象，如果转换失败或输入为null，则返回null
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseDate(str.toString(), PARSE_PATTERNS);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 获取服务器启动时间
     *
     * @return 服务器启动时间的 Date 对象表示
     */
    public static Date getServerStartDate() {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算两个时间之间的时间差，并以指定单位返回（绝对值）
     *
     * @param start 起始时间
     * @param end   结束时间
     * @param unit  所需返回的时间单位（DAYS、HOURS、MINUTES、SECONDS、MILLISECONDS、MICROSECONDS、NANOSECONDS）
     * @return 时间差的绝对值，以指定单位表示
     */
    public static long difference(Date start, Date end, TimeUnit unit) {
        // 计算时间差，单位为毫秒，取绝对值避免负数
        long diffInMillis = Math.abs(end.getTime() - start.getTime());

        // 根据目标单位转换时间差
        return switch (unit) {
            case DAYS -> diffInMillis / TimeUnit.DAYS.toMillis(1);
            case HOURS -> diffInMillis / TimeUnit.HOURS.toMillis(1);
            case MINUTES -> diffInMillis / TimeUnit.MINUTES.toMillis(1);
            case SECONDS -> diffInMillis / TimeUnit.SECONDS.toMillis(1);
            case MILLISECONDS -> diffInMillis;
            case MICROSECONDS -> TimeUnit.MILLISECONDS.toMicros(diffInMillis);
            case NANOSECONDS -> TimeUnit.MILLISECONDS.toNanos(diffInMillis);
        };
    }

    /**
     * 计算两个日期之间的时间差，并以天、小时和分钟的格式返回
     *
     * @param endDate 结束日期
     * @param nowDate 当前日期
     * @return 表示时间差的字符串，格式为"天 小时 分钟"
     */
    public static String getDatePoor(Date endDate, Date nowDate) {
        long diffInMillis = endDate.getTime() - nowDate.getTime();
        long day = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hour = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24;
        long min = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
        return String.format("%d天 %d小时 %d分钟", day, hour, min);
    }

    /**
     * 计算两个时间点的差值（天、小时、分钟、秒），当值为0时不显示该单位
     *
     * @param endDate 结束时间
     * @param nowDate 当前时间
     * @return 时间差字符串，格式为 "x天 x小时 x分钟 x秒"，若为 0 则不显示
     */
    public static String getTimeDifference(Date endDate, Date nowDate) {
        long diffInMillis = endDate.getTime() - nowDate.getTime();
        long day = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long hour = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24;
        long min = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
        long sec = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;
        // 构建时间差字符串，条件是值不为0才显示
        StringBuilder result = new StringBuilder();
        if (day > 0) {
            result.append(String.format("%d天 ", day));
        }
        if (hour > 0) {
            result.append(String.format("%d小时 ", hour));
        }
        if (min > 0) {
            result.append(String.format("%d分钟 ", min));
        }
        if (sec > 0) {
            result.append(String.format("%d秒", sec));
        }
        return result.length() > 0 ? result.toString().trim() : "0秒";
    }

    /**
     * 将 LocalDateTime 对象转换为 Date 对象
     *
     * @param temporalAccessor 要转换的 LocalDateTime 对象
     * @return 转换后的 Date 对象
     */
    public static Date toDate(LocalDateTime temporalAccessor) {
        ZonedDateTime zdt = temporalAccessor.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 将 LocalDate 对象转换为 Date 对象
     *
     * @param temporalAccessor 要转换的 LocalDate 对象
     * @return 转换后的 Date 对象
     */
    public static Date toDate(LocalDate temporalAccessor) {
        LocalDateTime localDateTime = LocalDateTime.of(temporalAccessor, LocalTime.of(0, 0, 0));
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 校验日期范围
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param maxValue  最大时间跨度的限制值
     * @param unit      时间跨度的单位，可选择 "DAYS"、"HOURS" 或 "MINUTES"
     */
    public static void validateDateRange(Date startDate, Date endDate, int maxValue, TimeUnit unit) {
        // 校验结束日期不能早于开始日期
        if (endDate.before(startDate)) {
            throw new ServiceException("结束日期不能早于开始日期");
        }

        // 计算时间跨度
        long diffInMillis = endDate.getTime() - startDate.getTime();

        // 根据单位转换时间跨度
        long diff = switch (unit) {
            case DAYS -> TimeUnit.MILLISECONDS.toDays(diffInMillis);
            case HOURS -> TimeUnit.MILLISECONDS.toHours(diffInMillis);
            case MINUTES -> TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            default -> throw new IllegalArgumentException("不支持的时间单位");
        };

        // 校验时间跨度不超过最大限制
        if (diff > maxValue) {
            throw new ServiceException("最大时间跨度为 {} {}", maxValue, unit.toString().toLowerCase());
        }
    }

    /**
     * 根据指定日期时间获取时间段（凌晨 / 上午 / 中午 / 下午 / 晚上）
     *
     * @param date 日期时间
     * @return 时间段描述
     */
    public static String getTodayHour(Date date) {
        int hour = DateUtil.hour(date, true);
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
        Date now = DateUtil.date();

        // 未来时间或非今年
        if (date.after(now) || DateUtil.year(date) != DateUtil.year(now)) {
            return parseDateToStr(FormatsType.YYYY_MM_DD_HH_MM, date);
        }

        // 今天
        if (DateUtil.isSameDay(date, now)) {
            long minutes = DateUtil.between(date, now, DateUnit.MINUTE);
            if (minutes < 1) {
                return "刚刚";
            }
            if (minutes < 60) {
                return minutes + "分钟前";
            }
            return getTodayHour(date) + " " + DateUtil.format(date, "HH:mm");
        }

        // 昨天
        if (DateUtil.isSameDay(date, DateUtil.yesterday())) {
            return "昨天 " + DateUtil.format(date, "HH:mm");
        }

        // 本周
        if (DateUtil.isSameWeek(date, now, true)) {
            return DateUtil.dayOfWeekEnum(date).toChinese("周")
                + " " + DateUtil.format(date, "HH:mm");
        }

        // 今年内其它时间
        return DateUtil.format(date, "MM-dd HH:mm");
    }

}
