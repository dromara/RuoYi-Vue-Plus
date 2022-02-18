package com.ruoyi.demo.controller.queue;

import java.util.Comparator;

/**
 * 比较器 注意不允许使用 内部类或匿名类或lambda表达式 会找不到类
 *
 * @author Lion Li
 * @version 3.6.0
 */
public class PriorityDemoComparator implements Comparator<PriorityDemo> {
    @Override
    public int compare(PriorityDemo pd1, PriorityDemo pd2) {
        return Integer.compare(pd1.getOrderNum(), pd2.getOrderNum());
    }
}
