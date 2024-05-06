package org.dromara.datasource.jdbc.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据类型
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataType {
    private String name;
    private boolean signed;
}
