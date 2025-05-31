package org.dromara.common.core.utils.ip;

import cn.hutool.http.HtmlUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.NetUtils;
import org.dromara.common.core.utils.StringUtils;

/**
 * 获取地址类
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressUtils {

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        // 处理空串并过滤HTML标签
        ip = HtmlUtil.cleanHtmlTag(StringUtils.blankToDefault(ip,""));
        boolean isIPv6 = NetUtils.isIPv6(ip);
        // 判断是否为IPv4或IPv6，如果不是则返回未知地址
        if (!NetUtils.isIPv4(ip) && !isIPv6) {
            return UNKNOWN;
        }
        // 内网不查询
        if (NetUtils.isInnerIPv6(ip) || NetUtils.isInnerIP(ip)) {
            return "内网IP";
        }
        // 不支持IPv6，不再进行没有必要的IP地址信息的解析，直接返回
        if (isIPv6) {
            log.warn("ip2region不支持IPV6地址解析：{}", ip);
            // 如有需要，可自行实现IPv6地址信息解析逻辑，并在这里返回
            return "未知";
        }
        return RegionUtils.getCityInfo(ip);
    }

}
