package com.ruoyi.system.domain;

import com.ruoyi.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存信息
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@Schema(name = "缓存信息")
public class SysCache {

    /**
     * 缓存名称
     */
    @Schema(name = "缓存名称")
    private String cacheName = "";

    /**
     * 缓存键名
     */
    @Schema(name = "缓存键名")
    private String cacheKey = "";

    /**
     * 缓存内容
     */
    @Schema(name = "缓存内容")
    private String cacheValue = "";

    /**
     * 备注
     */
    @Schema(name = "备注")
    private String remark = "";

    public SysCache(String cacheName, String remark) {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    public SysCache(String cacheName, String cacheKey, String cacheValue) {
        this.cacheName = StringUtils.replace(cacheName, ":", "");
        this.cacheKey = StringUtils.replace(cacheKey, cacheName, "");
        this.cacheValue = cacheValue;
    }

}
