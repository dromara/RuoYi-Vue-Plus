package com.ruoyi.demo.controller.queue;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 实体类 注意不允许使用内部类 否则会找不到类
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class PriorityDemo {
    private String name;
    private Integer orderNum;
}
