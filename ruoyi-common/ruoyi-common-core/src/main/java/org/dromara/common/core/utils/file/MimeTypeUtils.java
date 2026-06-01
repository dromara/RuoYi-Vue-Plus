package org.dromara.common.core.utils.file;

/**
 * 媒体类型工具类
 *
 * @author ruoyi
 */
public class MimeTypeUtils {

    /**
     * PNG 图片 MIME 类型。
     */
    public static final String IMAGE_PNG = "image/png";

    /**
     * JPG 图片 MIME 类型。
     */
    public static final String IMAGE_JPG = "image/jpg";

    /**
     * JPEG 图片 MIME 类型。
     */
    public static final String IMAGE_JPEG = "image/jpeg";

    /**
     * BMP 图片 MIME 类型。
     */
    public static final String IMAGE_BMP = "image/bmp";

    /**
     * GIF 图片 MIME 类型。
     */
    public static final String IMAGE_GIF = "image/gif";

    /**
     * 图片扩展名集合。
     */
    public static final String[] IMAGE_EXTENSION = {"bmp", "gif", "jpg", "jpeg", "png"};

    /**
     * Flash 扩展名集合。
     */
    public static final String[] FLASH_EXTENSION = {"swf", "flv"};

    /**
     * 媒体扩展名集合。
     */
    public static final String[] MEDIA_EXTENSION = {"swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg",
        "asf", "rm", "rmvb"};

    /**
     * 视频扩展名集合。
     */
    public static final String[] VIDEO_EXTENSION = {"mp4", "avi", "rmvb"};

    /**
     * 默认允许上传扩展名集合。
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
        "pdf"};

}
