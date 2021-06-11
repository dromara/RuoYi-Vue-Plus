package com.ruoyi.common.core.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 分页 Page 增强对象
 *
 * @param <T> 数据库实体
 * @param <K> vo实体
 * @author Lion Li
 */
@Data
@Accessors(chain = true)
public class PagePlus<T,K> implements IPage<T> {

    /**
     * domain实体列表
     */
    private List<T> records = Collections.emptyList();

    /**
     * vo实体列表
     */
    private List<K> recordsVo = Collections.emptyList();

    /**
     * 总数
     */
    private long total = 0L;

    /**
     * 页长度
     */
    private long size = 10L;

    /**
     * 当前页
     */
    private long current = 1L;

    /**
     * 排序字段信息
     */
    private List<OrderItem> orders = new ArrayList<>();

    /**
     * 自动优化 COUNT SQL
     */
    private boolean optimizeCountSql = true;

    /**
     * 是否进行 count 查询
     */
    private boolean isSearchCount = true;

    /**
     * 是否命中count缓存
     */
    private boolean hitCount = false;

    /**
     * countId
     */
    private String countId;

    /**
     * 最大limit
     */
    private Long maxLimit;

    public PagePlus() {
    }

    public PagePlus(long current, long size) {
        this(current, size, 0L);
    }

    public PagePlus(long current, long size, long total) {
        this(current, size, total, true);
    }

    public PagePlus(long current, long size, boolean isSearchCount) {
        this(current, size, 0L, isSearchCount);
    }

    public PagePlus(long current, long size, long total, boolean isSearchCount) {
        if (current > 1L) {
            this.current = current;
        }
        this.size = size;
        this.total = total;
        this.isSearchCount = isSearchCount;
    }

    @Override
    public String countId() {
        return this.getCountId();
    }

    @Override
    public Long maxLimit() {
        return this.getMaxLimit();
    }

    public PagePlus<T, K> addOrder(OrderItem... items) {
        this.orders.addAll(Arrays.asList(items));
        return this;
    }

    public PagePlus<T, K> addOrder(List<OrderItem> items) {
        this.orders.addAll(items);
        return this;
    }

    @Override
    public List<OrderItem> orders() {
        return this.getOrders();
    }

    @Override
    public boolean optimizeCountSql() {
        return this.optimizeCountSql;
    }

	@Override
	public long getPages() {
		// 解决 github issues/3208
		return IPage.super.getPages();
	}

	public static <T,K> PagePlus<T,K> of(long current, long size) {
		return of(current, size, 0);
	}

	public static <T,K> PagePlus<T,K> of(long current, long size, long total) {
		return of(current, size, total, true);
	}

	public static <T,K> PagePlus<T,K> of(long current, long size, boolean searchCount) {
		return of(current, size, 0, searchCount);
	}

	public static <T,K> PagePlus<T,K> of(long current, long size, long total, boolean searchCount) {
		return new PagePlus<>(current, size, total, searchCount);
	}

}

