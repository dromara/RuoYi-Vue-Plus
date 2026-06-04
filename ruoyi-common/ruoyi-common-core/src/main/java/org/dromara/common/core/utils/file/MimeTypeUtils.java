package org.dromara.common.core.utils.file;

import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 媒体类型工具类
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MimeTypeUtils {

    /**
     * PNG 图片 MIME 类型
     */
    public static final String IMAGE_PNG = "image/png";

    /**
     * JPG 图片 MIME 类型
     */
    public static final String IMAGE_JPG = "image/jpg";

    /**
     * JPEG 图片 MIME 类型
     */
    public static final String IMAGE_JPEG = "image/jpeg";

    /**
     * BMP 图片 MIME 类型
     */
    public static final String IMAGE_BMP = "image/bmp";

    /**
     * GIF 图片 MIME 类型
     */
    public static final String IMAGE_GIF = "image/gif";

    /**
     * 图片扩展名数组
     */
    public static final String[] IMAGE_EXTENSION = {"bmp", "gif", "jpg", "jpeg", "png"};

    /**
     * Flash 文件扩展名数组
     */
    public static final String[] FLASH_EXTENSION = {"swf", "flv"};

    /**
     * 视频文件扩展名数组
     */
    public static final String[] VIDEO_EXTENSION = {"mp4", "avi", "rmvb"};

    /**
     * 媒体文件扩展名数组（音频+视频+Flash）
     */
    public static final String[] MEDIA_EXTENSION = {"swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg", "asf", "rm", "rmvb"};

    /**
     * 默认允许上传的文件扩展名数组
     */
    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png",
            // word excel powerpoint
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt",
            // 压缩文件
            "rar", "zip", "gz", "bz2",
            // 视频格式
            "mp4", "avi", "rmvb",
            // pdf
            "pdf"
    };

    /**
     * 判断文件扩展名是否为图片格式（忽略大小写）
     *
     * @param extension 文件扩展名
     * @return 是图片返回 true，否则返回 false
     */
    public static boolean isImage(String extension) {
        return StrUtil.equalsAnyIgnoreCase(extension, IMAGE_EXTENSION);
    }

    /**
     * 判断文件扩展名是否为视频格式（忽略大小写）
     *
     * @param extension 文件扩展名
     * @return 是视频返回 true，否则返回 false
     */
    public static boolean isVideo(String extension) {
        return StrUtil.equalsAnyIgnoreCase(extension, VIDEO_EXTENSION);
    }

    /**
     * 判断文件扩展名是否为媒体格式（忽略大小写）
     *
     * @param extension 文件扩展名
     * @return 是媒体返回 true，否则返回 false
     */
    public static boolean isMedia(String extension) {
        return StrUtil.equalsAnyIgnoreCase(extension, MEDIA_EXTENSION);
    }

    /**
     * 判断文件扩展名是否在默认允许上传范围内（忽略大小写）
     *
     * @param extension 文件扩展名
     * @return 允许返回 true，不允许返回 false
     */
    public static boolean isDefaultAllowed(String extension) {
        return StrUtil.equalsAnyIgnoreCase(extension, DEFAULT_ALLOWED_EXTENSION);
    }

}
