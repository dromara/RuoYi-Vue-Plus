package org.dromara.common.oss.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.StringUtils;

/**
 * 桶链接工具类
 *
 * @author 秋辞未寒
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketUrlUtil {

    public static final String HTTP_PROTOCOL_HEADER = "http://";
    public static final String HTTPS_PROTOCOL_HEADER = "https://";

    public static final String EMPTY_STRING = "";

    // 路径风格 例：https://s3examples.com/images
    private static final String PATH_STYLE_HTTP_FORMATE = "http://%s/%s";
    private static final String PATH_STYLE_HTTPS_FORMATE = "https://%s/%s";

    // 站点风格 例：https://images.oss-cn-beijing.aliyuncs.com
    private static final String SITE_STYLE_HTTP_FORMATE = "http://%s.%s";
    private static final String SITE_STYLE_HTTPS_FORMATE = "https://%s.%s";

    /**
     * 重建链接协议头（将IP、域名、站点的协议头改成HTTP或者HTTPS）
     *
     * @param isHttps 是否为HTTP
     * @param base    基础地址（可以是IP、站点或者域名）
     * @return 域名地址
     */
    public static String rebuildUrlHeader(boolean isHttps, String base) {
        String baseUrl = removeHttpProtocolHeader(base);
        if (isHttps) {
            return HTTPS_PROTOCOL_HEADER + baseUrl;
        }
        return HTTP_PROTOCOL_HEADER + baseUrl;
    }

    /**
     * 获取路径风格的桶地址 例：https://s3examples.com/images
     *
     * @param isHttps    是否为HTTP
     * @param base       基础地址（可以是IP、站点或者域名）
     * @param bucketName 桶名称
     * @return 路径风格的桶地址
     */
    public static String getPathStyleBucketUrl(boolean isHttps, String base, String bucketName) {
        String baseUrl = removeHttpProtocolHeader(base);
        if (isHttps) {
            return String.format(PATH_STYLE_HTTPS_FORMATE, baseUrl, bucketName);
        }
        return String.format(PATH_STYLE_HTTP_FORMATE, baseUrl, bucketName);
    }

    /**
     * 获取站点风格的桶地址 例：https://images.oss-cn-beijing.aliyuncs.com
     *
     * @param isHttps    是否为HTTP
     * @param base       基础地址（可以是IP、站点或者域名）
     * @param bucketName 桶名称
     * @return 站点风格的桶地址
     */
    public static String getSiteStyleBucketUrl(boolean isHttps, String base, String bucketName) {
        String baseUrl = removeHttpProtocolHeader(base);
        if (isHttps) {
            return String.format(SITE_STYLE_HTTPS_FORMATE, bucketName, baseUrl);
        }
        return String.format(SITE_STYLE_HTTP_FORMATE, bucketName, baseUrl);
    }

    /**
     * 移除HTTP/HTTPS协议头（如果有的话）
     *
     * @param url 链接地址
     * @return 移除HTTP/HTTPS协议头后的地址
     */
    public static String removeHttpProtocolHeader(String url) {
        if (StringUtils.startsWithIgnoreCase(url, HTTP_PROTOCOL_HEADER) || StringUtils.startsWithIgnoreCase(url, HTTPS_PROTOCOL_HEADER)) {
            return url.replace(HTTP_PROTOCOL_HEADER, EMPTY_STRING)
                .replace(HTTPS_PROTOCOL_HEADER, EMPTY_STRING);
        }
        return url;
    }
}
