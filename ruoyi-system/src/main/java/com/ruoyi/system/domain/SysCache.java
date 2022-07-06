package com.ruoyi.system.domain;

import com.ruoyi.common.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存信息
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@ApiModel("缓存信息")
public class SysCache {

    /**
     * 缓存名称
     */
    @ApiModelProperty(value = "缓存名称")
    private String cacheName = "";

    /**
     * 缓存键名
     */
    @ApiModelProperty(value = "缓存键名")
    private String cacheKey = "";

    /**
     * 缓存内容
     */
    @ApiModelProperty(value = "缓存内容")
    private String cacheValue = "";

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
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
