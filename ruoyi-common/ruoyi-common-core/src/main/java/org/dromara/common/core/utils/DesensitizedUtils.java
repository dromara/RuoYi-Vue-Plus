package org.dromara.common.core.utils;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 脱敏工具类
 *
 * @author AprilWind
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DesensitizedUtils extends DesensitizedUtil {

    /**
     * 灵活脱敏方法
     *
     * @param value         原始字符串
     * @param prefixVisible 前面可见长度
     * @param suffixVisible 后面可见长度
     * @param maskLength    中间掩码长度（固定显示多少 *，如果总长度不足则自动缩减）
     * @return 脱敏后字符串
     */
    public static String mask(String value, int prefixVisible, int suffixVisible, int maskLength) {
        if (StrUtil.isBlank(value)) {
            return value;
        }

        int len = value.length();
        int prefixMaskLimit = prefixVisible + maskLength;
        int fullLimit = prefixMaskLimit + suffixVisible;

        // 规则 1：长度 <= 中间掩码长度 → 全掩码
        if (len <= maskLength) {
            return StrUtil.repeat('*', len);
        }
        String mask = StrUtil.repeat('*', maskLength);

        // 规则 2：长度 <= 前缀 + 中间掩码
        if (len <= prefixMaskLimit) {
            return value.substring(0, len - maskLength) + mask;
        }

        String prefix = value.substring(0, prefixVisible);

        // 规则 3：长度 <= 前缀 + 中间掩码 + 后缀
        if (len <= fullLimit) {
            int suffixLen = len - prefixMaskLimit;
            return prefix + mask + value.substring(len - suffixLen);
        }

        // 规则 4：标准形态
        return prefix + mask + value.substring(len - suffixVisible);
    }

    /**
     * 高安全级别脱敏方法（Token / 私钥）
     *
     * @param value         原始字符串
     * @param prefixVisible 前面可见长度（推荐0~4）
     * @param suffixVisible 后面可见长度（推荐0~4）
     * @return 脱敏后字符串
     */
    public static String maskHighSecurity(String value, int prefixVisible, int suffixVisible) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        int len = value.length();

        // 规则1：长度 <= 前缀可见长度 → 全部掩码
        if (len <= prefixVisible) {
            return StrUtil.repeat('*', len);
        }

        // 规则2：长度 <= 前缀 + 后缀可见长度 → 优先掩码后面
        if (len <= prefixVisible + suffixVisible) {
            return value.substring(0, len - prefixVisible) + StrUtil.repeat('*', prefixVisible);
        }

        // 规则3：标准形态 → 前后可见，中间全部掩码
        return value.substring(0, prefixVisible)
            + StrUtil.repeat('*', len - prefixVisible - suffixVisible)
            + value.substring(len - suffixVisible);
    }

}
