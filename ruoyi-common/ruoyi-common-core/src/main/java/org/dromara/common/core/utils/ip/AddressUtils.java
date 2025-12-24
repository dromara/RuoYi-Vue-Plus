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

    // 未知IP
    public static final String UNKNOWN_IP = "XX XX";
    // 内网地址
    public static final String LOCAL_ADDRESS = "内网IP";

    public static String getRealAddressByIP(String ip) {
        // 处理空串并过滤HTML标签
        ip = HtmlUtil.cleanHtmlTag(StringUtils.blankToDefault(ip,""));
        // 判断是否为IPv4
        boolean isIPv4 = NetUtils.isIPv4(ip);
        // 判断是否为IPv6
        boolean isIPv6 = NetUtils.isIPv6(ip);
        // 如果不是IPv4或IPv6，则返回未知IP
        if (!isIPv4 && !isIPv6) {
            return UNKNOWN_IP;
        }
        // 内网不查询
        if ((isIPv4 && NetUtils.isInnerIP(ip)) || (isIPv6 && NetUtils.isInnerIPv6(ip))) {
            return LOCAL_ADDRESS;
        }
        // Tips：Ip2Region 提供了精简的IPv6地址库，精简的IPv6地址库并不能完全支持IPv6地址的查询，且准确度上可能会存在问题，如需要准确的IPv6地址查询，建议自行实现
        return RegionUtils.getRegion(ip);
    }

}
