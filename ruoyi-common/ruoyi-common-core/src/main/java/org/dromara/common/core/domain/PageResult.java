package org.dromara.common.core.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * 表格分页数据对象
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 列表数据
     */
    private Collection<T> rows;

    /**
     * 分页
     *
     * @param list  列表数据
     * @param total 总记录数
     */
    public PageResult(Collection<T> list, long total) {
        this.rows = emptyIfNull(list);
        this.total = total;
    }

    /**
     * 根据分页对象构建表格分页数据对象
     */
    public static <T> PageResult<T> build(Collection<T> list, long total) {
        PageResult<T> rspData = new PageResult<>();
        rspData.setRows(emptyIfNull(list));
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 根据数据列表构建表格分页数据对象
     */
    public static <T> PageResult<T> build(Collection<T> list) {
        PageResult<T> rspData = new PageResult<>();
        Collection<T> rows = emptyIfNull(list);
        rspData.setRows(rows);
        rspData.setTotal(rows.size());
        return rspData;
    }

    /**
     * 构建表格分页数据对象
     */
    public static <T> PageResult<T> build() {
        return new PageResult<>();
    }

    private static <T> Collection<T> emptyIfNull(Collection<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

}
