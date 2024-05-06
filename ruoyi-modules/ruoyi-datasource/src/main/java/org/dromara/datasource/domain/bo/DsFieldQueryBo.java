package org.dromara.datasource.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 数据源数据查询请求
 *
 * @author xyxj xieyangxuejun@gmail.com
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DsFieldQueryBo extends FieldQueryBo{

    @NotBlank(message = "数据源名称不能为空")
    private String dsName;

    @NotBlank(message = "数据库名称不能为空")
    private String schema;

    @NotBlank(message = "数据表名称不能为空")
    private String table;
}
