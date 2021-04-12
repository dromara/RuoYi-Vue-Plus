package com.ruoyi.common.core.page;

import cn.hutool.core.util.StrUtil;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 分页数据
 * 
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class PageDomain
{
    /** 当前记录起始索引 */
    private Integer pageNum;

    /** 每页显示记录数 */
    private Integer pageSize;

    /** 排序列 */
    private String orderByColumn;

    /** 排序的方向desc或者asc */
    private String isAsc = "asc";

    public String getOrderBy()
    {
        if (StrUtil.isEmpty(orderByColumn))
        {
            return "";
        }
        return StrUtil.toUnderlineCase(orderByColumn) + " " + isAsc;
    }

}
