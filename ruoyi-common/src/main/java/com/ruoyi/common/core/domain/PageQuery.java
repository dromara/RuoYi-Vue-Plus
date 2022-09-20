package com.ruoyi.common.core.domain;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.sql.SqlUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页查询实体类
 *
 * @author Lion Li
 */

@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分页大小
     */
    private Integer pageSize;

    /**
     * 当前页数
     */
    private Integer pageNum;

    /**
     * 排序列
     */
    private String orderByColumn;

    /**
     * 排序的方向desc或者asc
     */
    private String isAsc;

    /**
     * 当前记录起始索引 默认值
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 每页显示记录数 默认值 默认查全部
     */
    public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;

    public <T> Page<T> build() {
        Integer pageNum = ObjectUtil.defaultIfNull(getPageNum(), DEFAULT_PAGE_NUM);
        Integer pageSize = ObjectUtil.defaultIfNull(getPageSize(), DEFAULT_PAGE_SIZE);
        if (pageNum <= 0) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        Page<T> page = new Page<>(pageNum, pageSize);
        List<OrderItem> orderItems = buildOrderItem();
        if (ObjectUtil.isNotNull(orderItems)) {
            page.addOrder(orderItems);
        }
        return page;
    }

    private List<OrderItem> buildOrderItem() {
        if (StringUtils.isNotBlank(isAsc) && StringUtils.isNotBlank(orderByColumn)) {
            // 兼容前端排序类型
            isAsc = isAsc.replace("ascending", "asc")
                .replace("descending", "desc");

            String orderBy = SqlUtil.escapeOrderBySql(orderByColumn);
            orderBy = StringUtils.toUnderScoreCase(orderBy);
            //分割orderByColumn和isAsc参数
            String[] orderByArr = orderBy.split(",");
            String[] isAscArr = isAsc.split(",");
            if (isAscArr.length != 1 && isAscArr.length != orderByArr.length) {
                throw new ServiceException("排序参数有误");
            }
            if (isAscArr.length == 1) {
                //isAsc只提供了一个 则全部字段都是如此排序
                if ("asc".equals(isAsc)) {
                    return OrderItem.ascs(orderBy);
                } else if ("desc".equals(isAsc)) {
                    return OrderItem.descs(orderBy);
                } else {
                    throw new ServiceException("isAsc参数有误");
                }
            }
            ArrayList<OrderItem> list = new ArrayList<>();
            //每个字段各自排序
            for (int i = 0; i < isAscArr.length; i++) {
                String isAscStr = isAscArr[i];
                String orderByStr = orderByArr[i];
                if ("asc".equals(isAscStr)) {
                    list.add(OrderItem.asc(orderByStr));
                } else if ("desc".equals(isAscStr)) {
                    list.add(OrderItem.desc(orderByStr));
                } else {
                    throw new ServiceException("isAsc参数有误");
                }
            }
            return list;
        }
        return null;
    }

}
