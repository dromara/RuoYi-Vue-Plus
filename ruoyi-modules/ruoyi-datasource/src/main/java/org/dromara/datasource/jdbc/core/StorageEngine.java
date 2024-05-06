package org.dromara.datasource.jdbc.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * StorageEngine
 * 储存引擎
 *
 * @author xyxj xieyangxuejun@gmail.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StorageEngine {
    private String name;
    private int isDefault;
    private String comment;
    private List<Map<String, Object>> params;
}
