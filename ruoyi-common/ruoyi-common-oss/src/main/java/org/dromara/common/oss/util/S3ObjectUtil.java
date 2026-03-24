package org.dromara.common.oss.util;

import cn.hutool.core.util.IdUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.StringUtils;

/**
 * S3文件对象工具类
 *
 * @author 秋辞未寒
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class S3ObjectUtil {

    /**
     * 生成一个 【自定义前缀 + 日期路径 + SimpleUUID.文件后缀】 的对象Key 示例： images/20260321/019d0f89c9b1130a48c90dbca0475a.jpg
     *
     * @param prefix 前缀
     * @param withSuffixFileName 带后缀的文件名
     * @return 文件路径对象Key
     */
    public static String buildPathKey(String prefix, String withSuffixFileName) {
        // 获取后缀
        String suffix = StringUtils.substring(withSuffixFileName, withSuffixFileName.lastIndexOf("."), withSuffixFileName.length());
        // 生成日期路径
        String datePath = DateUtils.datePath();
        // 生成uuid
        String uuid = IdUtil.fastSimpleUUID();
        // 拼接路径
        String path = StringUtils.isNotEmpty(prefix) ? prefix + StringUtils.SLASH + datePath + StringUtils.SLASH + uuid : datePath + StringUtils.SLASH + uuid;
        return path + suffix;
    }

    /**
     * 生成一个 【日期路径 + SimpleUUID.文件后缀】 的对象Key 示例： 20260321/019d0f89c9b1130a48c90dbca0475a.jpg
     *
     * @param withSuffixFileName 带后缀的文件名
     * @return 文件路径对象Key
     */
    public static String buildPathKey(String withSuffixFileName) {
        return buildPathKey("", withSuffixFileName);
    }

}
