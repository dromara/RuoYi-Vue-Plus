package com.ruoyi.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.sql.SqlUtil;

import java.util.List;

/**
 * 分页工具
 *
 * @author Lion Li
 */
public class PageUtils {

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 当前记录起始索引 默认值
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 每页显示记录数 默认值 默认查全部
     */
    public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;

    /**
     * 构建 plus 分页对象
     * @param <T> domain 实体
     * @param <K> vo 实体
     * @return 分页对象
     */
    public static <T, K> PagePlus<T, K> buildPagePlus() {
        Integer pageNum = ServletUtils.getParameterToInt(PAGE_NUM, DEFAULT_PAGE_NUM);
        Integer pageSize = ServletUtils.getParameterToInt(PAGE_SIZE, DEFAULT_PAGE_SIZE);
        String orderByColumn = ServletUtils.getParameter(ORDER_BY_COLUMN);
        String isAsc = ServletUtils.getParameter(IS_ASC);
        PagePlus<T, K> page = new PagePlus<>(pageNum, pageSize);
        if (StrUtil.isNotBlank(orderByColumn)) {
            String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
            if ("asc".equals(isAsc)) {
                page.addOrder(OrderItem.asc(orderBy));
            } else if ("desc".equals(isAsc)) {
                page.addOrder(OrderItem.desc(orderBy));
            }
        }
        return page;
    }

	public static <T> Page<T> buildPage() {
		return buildPage(null, null);
	}

    /**
     * 构建 MP 普通分页对象
     * @param <T> domain 实体
     * @return 分页对象
     */
    public static <T> Page<T> buildPage(String defaultOrderByColumn, String defaultIsAsc) {
        Integer pageNum = ServletUtils.getParameterToInt(PAGE_NUM, DEFAULT_PAGE_NUM);
        Integer pageSize = ServletUtils.getParameterToInt(PAGE_SIZE, DEFAULT_PAGE_SIZE);
        String orderByColumn = ServletUtils.getParameter(ORDER_BY_COLUMN, defaultOrderByColumn);
        String isAsc = ServletUtils.getParameter(IS_ASC, defaultIsAsc);
		// 兼容前端排序类型
		if ("ascending".equals(isAsc)) {
			isAsc = "asc";
		} else if ("descending".equals(isAsc)) {
			isAsc = "desc";
		}
        Page<T> page = new Page<>(pageNum, pageSize);
        if (StrUtil.isNotBlank(orderByColumn)) {
            String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
			orderBy = StrUtil.toUnderlineCase(orderBy);
			if ("asc".equals(isAsc)) {
                page.addOrder(OrderItem.asc(orderBy));
            } else if ("desc".equals(isAsc)) {
                page.addOrder(OrderItem.desc(orderBy));
            }
        }
        return page;
    }

    public static <T, K> TableDataInfo<K> buildDataInfo(PagePlus<T, K> page) {
        TableDataInfo<K> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecordsVo());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    public static <T> TableDataInfo<T> buildDataInfo(Page<T> page) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    public static <T> TableDataInfo<T> buildDataInfo(List<T> list) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(list.size());
        return rspData;
    }

}
