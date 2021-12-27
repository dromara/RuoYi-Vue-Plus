package com.ruoyi.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.sql.SqlUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页工具
 *
 * @author Lion Li
 * @deprecated 3.6.0 删除 请使用 {@link PageQuery} 与 {@link TableDataInfo}
 */
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PageUtils {

    /**
     * 当前记录起始索引
     */
    @Deprecated
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    @Deprecated
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    @Deprecated
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    @Deprecated
    public static final String IS_ASC = "isAsc";

    /**
     * 当前记录起始索引 默认值
     */
    @Deprecated
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 每页显示记录数 默认值 默认查全部
     */
    @Deprecated
    public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;

    /**
     * 构建 plus 分页对象
     *
     * @param <T> domain 实体
     * @param <K> vo 实体
     * @return 分页对象
     * @deprecated 3.6.0 删除 请使用 {@link PageQuery#build()}
     * 由于使用 Servlet 获取只能从 param 获取 灵活性降低 故将传参操作交给用户
     */
    @Deprecated
    public static <T, K> PagePlus<T, K> buildPagePlus() {
        Integer pageNum = ServletUtils.getParameterToInt(PAGE_NUM, DEFAULT_PAGE_NUM);
        Integer pageSize = ServletUtils.getParameterToInt(PAGE_SIZE, DEFAULT_PAGE_SIZE);
        String orderByColumn = ServletUtils.getParameter(ORDER_BY_COLUMN);
        String isAsc = ServletUtils.getParameter(IS_ASC);
        if (pageNum <= 0) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        PagePlus<T, K> page = new PagePlus<>(pageNum, pageSize);
        OrderItem orderItem = buildOrderItem(orderByColumn, isAsc);
        if (ObjectUtil.isNotNull(orderItem)) {
            page.addOrder(orderItem);
        }
        return page;
    }

    @Deprecated
    public static <T> Page<T> buildPage() {
        return buildPage(null, null);
    }

    /**
     * 构建 MP 普通分页对象
     *
     * @param <T> domain 实体
     * @return 分页对象
     * @deprecated 3.6.0 删除 请使用 {@link PageQuery#build()}
     * 由于使用 Servlet 获取只能从 param 获取 灵活性降低 故将传参操作交给用户
     */
    @Deprecated
    public static <T> Page<T> buildPage(String defaultOrderByColumn, String defaultIsAsc) {
        Integer pageNum = ServletUtils.getParameterToInt(PAGE_NUM, DEFAULT_PAGE_NUM);
        Integer pageSize = ServletUtils.getParameterToInt(PAGE_SIZE, DEFAULT_PAGE_SIZE);
        String orderByColumn = ServletUtils.getParameter(ORDER_BY_COLUMN, defaultOrderByColumn);
        String isAsc = ServletUtils.getParameter(IS_ASC, defaultIsAsc);
        if (pageNum <= 0) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        Page<T> page = new Page<>(pageNum, pageSize);
        OrderItem orderItem = buildOrderItem(orderByColumn, isAsc);
        if (ObjectUtil.isNotNull(orderItem)) {
            page.addOrder(orderItem);
        }
        return page;
    }

    private static OrderItem buildOrderItem(String orderByColumn, String isAsc) {
        // 兼容前端排序类型
        if ("ascending".equals(isAsc)) {
            isAsc = "asc";
        } else if ("descending".equals(isAsc)) {
            isAsc = "desc";
        }
        if (StringUtils.isNotBlank(orderByColumn)) {
            String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
            orderBy = StringUtils.toUnderScoreCase(orderBy);
            if ("asc".equals(isAsc)) {
                return OrderItem.asc(orderBy);
            } else if ("desc".equals(isAsc)) {
                return OrderItem.desc(orderBy);
            }
        }
        return null;
    }

    /**
     * 构建 MP 普通分页对象
     *
     * @param <T> domain 实体
     * @return 分页对象
     * @deprecated 3.6.0 删除 请使用 {@link PageQuery#build()}
     * 由于使用 Servlet 获取只能从 param 获取 灵活性降低 故将传参操作交给用户
     */
    @Deprecated
    public static <T, K> TableDataInfo<K> buildDataInfo(PagePlus<T, K> page) {
        TableDataInfo<K> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecordsVo());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    /**
     * @deprecated 3.6.0 删除 请使用 {@link TableDataInfo#build(IPage)}
     */
    @Deprecated
    public static <T> TableDataInfo<T> buildDataInfo(Page<T> page) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    /**
     * @deprecated 3.6.0 删除 请使用 {@link TableDataInfo#build(List)}
     */
    @Deprecated
    public static <T> TableDataInfo<T> buildDataInfo(List<T> list) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(list.size());
        return rspData;
    }

    /**
     * @deprecated 3.6.0 删除 请使用 {@link TableDataInfo#build()}
     */
    @Deprecated
    public static <T> TableDataInfo<T> buildDataInfo() {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HttpStatus.HTTP_OK);
        rspData.setMsg("查询成功");
        return rspData;
    }

}
