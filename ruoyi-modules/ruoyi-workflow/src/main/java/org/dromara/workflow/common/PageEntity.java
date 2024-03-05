package org.dromara.workflow.common;


import lombok.Data;

/**
 * 分页参数
 *
 * @author may
 */
@Data
public class PageEntity {

    /**
     * 当前页码
     */
    private Integer pageNum = 0;

    /**
     * 页容量
     */
    private Integer pageSize = 10;

    public Integer getPageNum() {
        return (pageNum - 1) * pageSize;
    }

}
