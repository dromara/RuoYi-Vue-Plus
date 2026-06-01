package org.dromara.demo.controller.queue;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实体类 注意不允许使用内部类 否则会找不到类
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Data
@NoArgsConstructor
public class PriorityDemo implements Comparable<PriorityDemo> {
    /**
     * 队列元素名称。
     */
    private String name;

    /**
     * 优先级排序值。
     */
    private Integer orderNum;

    /**
     * 按排序值比较优先级。
     *
     * @param other 另一个队列元素
     * @return 比较结果
     */
    @Override
    public int compareTo(PriorityDemo other) {
        return Integer.compare(getOrderNum(), other.getOrderNum());
    }
}
