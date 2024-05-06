package org.dromara.datasource.jdbc.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * InternalFunctions
 * 内置函数
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalFunction {
    private String name;
    private String category;
    private String description;
    private String syntax;
}
