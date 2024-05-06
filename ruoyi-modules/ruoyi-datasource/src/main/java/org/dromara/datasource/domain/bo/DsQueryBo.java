package org.dromara.datasource.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源查询请求
 *
 * @author xyxj xieyangxuejun@gmail.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DsQueryBo {

    @NotBlank(message = "数据源名称不能为空")
    private String dsName;

    @NotBlank(message = "数据源类型不能为空")
    private String dsType;

    private String schema;

    private String table;

    private String sqlScript;
}
