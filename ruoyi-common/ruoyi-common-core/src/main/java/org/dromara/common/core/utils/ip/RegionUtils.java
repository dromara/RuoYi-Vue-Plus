package org.dromara.common.core.utils.ip;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.xdb.Util;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;

/**
 * IP地址行政区域工具类
 * 参考地址：<a href="https://gitee.com/lionsoul/ip2region/tree/master/binding/java">ip2region xdb java 查询客户端实现</a>
 * xdb数据库文件下载：<a href="https://gitee.com/lionsoul/ip2region/tree/master/data">ip2region data</a>
 *
 * @author 秋辞未寒
 */
@Slf4j
public class RegionUtils {

    // 默认IPv4地址库文件路径
    // 下载地址：https://gitee.com/lionsoul/ip2region/blob/master/data/ip2region_v4.xdb
    public static final String DEFAULT_IPV4_XDB_PATH = "ip2region_v4.xdb";

    // 默认IPv6地址库文件路径
    // 下载地址：https://gitee.com/lionsoul/ip2region/blob/master/data/ip2region_v6.xdb
    public static final String DEFAULT_IPV6_XDB_PATH = "ip2region_v6.xdb";

    // 未知地址
    public static final String UNKNOWN_ADDRESS = "未知";

    // Ip2Region服务实例
    private static Ip2Region ip2Region;

    // 初始化Ip2Region服务实例
    static {
        try {
            // 注意：Ip2Region 的xdb文件加载策略 CachePolicy 有三种，分别是：BufferCache（全量读取xdb到内存中）、VIndexCache（默认策略，按需读取并缓存）、NoCache（实时读取）
            // 本项目工具使用的 CachePolicy 为 BufferCache，BufferCache会加载整个xdb文件到内存中，setXdbInputStream 仅支持 BufferCache 策略。
            // 因为加载整个xdb文件会耗费非常大的内存，如果你不希望加载整个xdb到内存中，更推荐使用 VIndexCache 或 NoCache（即实时读取文件）策略和 setXdbPath/setXdbFile 加载方法（需要注意的一点，setXdbPath 和 setXdbFile 不支持读取ClassPath（即源码和resource目录）中的文件）。
            // 一般而言，更建议把xdb数据库放到一个指定的文件目录中（即不打包进jar包中），然后使用 NoCache + 配合SearcherPool的并发池读取数据，更方便随时更新xdb数据库

            // TODO 2025年12月23日 Ip2Region封装的 InputStream 读取函数 Searcher.loadContentFromInputStream 在Linux环境下会申请过大的byte[]空间而导致OOM，这里先用临时文件的方案解决，等后续 Ip2Region 更新解决方案
            // 创建临时文件
            File v4TempXdb = FileUtil.writeFromStream(ResourceUtil.getStream(DEFAULT_IPV4_XDB_PATH), FileUtil.createTempFile());

            // IPv4配置
            Config v4Config = Config.custom()
                .setCachePolicy(Config.BufferCache)
                .setXdbFile(v4TempXdb)
//                .setXdbInputStream(ResourceUtil.getStream(DEFAULT_IPV4_XDB_PATH))
                .asV4();
            // 删除临时文件
            v4TempXdb.delete();

            // IPv6配置
            Config v6Config = null;
            InputStream v6XdbInputStream = ResourceUtil.getStreamSafe(DEFAULT_IPV6_XDB_PATH);
            if (v6XdbInputStream == null) {
                log.warn("未加载 IPv6 地址库：未在类路径下找到文件 {}。当前仅启用 IPv4 查询。如需启用 IPv6，请将 ip2region_v6.xdb 放置到 resources 目录", DEFAULT_IPV6_XDB_PATH);
            } else {
                // 创建临时文件
                File v6TempXdb = FileUtil.writeFromStream(ResourceUtil.getStream(DEFAULT_IPV4_XDB_PATH), FileUtil.createTempFile());

                v6Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbFile(v6TempXdb)
//                    .setXdbInputStream(v6XdbInputStream)
                    .asV6();

                // 删除临时文件
                v6TempXdb.delete();
            }

            // 初始化Ip2Region实例
            RegionUtils.ip2Region = Ip2Region.create(v4Config, v6Config);
            log.debug("IP工具初始化成功，加载IP地址库数据成功！");
        } catch (Exception e) {
            throw new ServiceException("RegionUtils初始化失败，原因：{}", e.getMessage());
        }
    }

    /**
     * 根据IP地址离线获取城市
     *
     * @param ipString ip地址字符串
     */
    public static String getRegion(String ipString) {
        try {
            String region = ip2Region.search(ipString);
            if (StringUtils.isBlank(region)) {
                region = UNKNOWN_ADDRESS;
            }
            return region;
        } catch (Exception e) {
            log.error("IP地址离线获取城市异常 {}", ipString);
            return UNKNOWN_ADDRESS;
        }

    }

    /**
     * 根据IP地址离线获取城市
     *
     * @param ipBytes ip地址字节数组
     */
    public static String getRegion(byte[] ipBytes) {
        try {
            String region = ip2Region.search(ipBytes);
            if (StringUtils.isBlank(region)) {
                region = UNKNOWN_ADDRESS;
            }
            return region;
        } catch (Exception e) {
            log.error("IP地址离线获取城市异常 {}", Util.ipToString(ipBytes));
            return UNKNOWN_ADDRESS;
        }
    }

    /**
     * 关闭Ip2Region服务
     */
    public static void close() {
        if (ip2Region == null) {
            return;
        }
        try {
            ip2Region.close(10000);
        } catch (Exception e) {
            log.error("Ip2Region服务关闭异常", e);
        }
    }

    /**
     * 关闭Ip2Region服务
     *
     * @param timeout 关闭超时时间
     */
    public static void close(final Duration timeout) {
        if (ip2Region == null) {
            return;
        }
        if (timeout == null) {
            close();
            return;
        }
        try {
            ip2Region.close(timeout.toMillis());
        } catch (Exception e) {
            log.error("Ip2Region服务关闭异常", e);
        }
    }

}
