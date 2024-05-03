package org.dromara.datasource.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.datasource.domain.SysDatasource;

import java.io.Serial;
import java.io.Serializable;



/**
 * 动态数据源视图对象 sys_datasource
 *
 * @author ixyxj
 * @date 2024-05-02
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDatasource.class)
public class SysDatasourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 数据源类型
     */
    @ExcelProperty(value = "数据源类型")
    private String dsType;

    /**
     * 数据源名称
     */
    @ExcelProperty(value = "数据源名称")
    private String dsName;

    /**
     * 数据源url
     */
    @ExcelProperty(value = "数据源url")
    private String connUrl;

    /**
     * 数据源用户名
     */
    @ExcelProperty(value = "数据源用户名")
    private String username;

    /**
     * 数据源密码
     */
    @ExcelProperty(value = "数据源密码")
    private String password;

    /**
     * 驱动类名
     */
    @ExcelProperty(value = "驱动类名")
    private String driverClassName;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
