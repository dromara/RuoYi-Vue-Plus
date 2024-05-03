package org.dromara.datasource.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.datasource.domain.SysDatasource;

/**
 * 动态数据源业务对象 sys_datasource
 *
 * @author ixyxj
 * @date 2024-05-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDatasource.class, reverseConvertGenerate = false)
public class SysDatasourceBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 数据源类型
     */
    @NotBlank(message = "数据源类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String dsType;

    /**
     * 数据源名称
     */
    @NotBlank(message = "数据源名称不能为空", groups = { EditGroup.class })
    private String dsName;

    @NotBlank(message = "数据源host不能为空", groups = { EditGroup.class })
    private String host;

    @NotBlank(message = "数据源端口不能为空", groups = { EditGroup.class })
    private String port;

    /**
     * 数据源url
     */
    @NotBlank(message = "数据源url不能为空", groups = { AddGroup.class, EditGroup.class })
    private String connUrl;

    /**
     * 数据源用户名
     */
    private String username;

    /**
     * 数据源密码
     */
    private String password;

    /**
     * 驱动类名
     */
    @NotBlank(message = "驱动类名不能为空", groups = { AddGroup.class, EditGroup.class })
    private String driverClassName;

    /**
     * 备注
     */
    private String remark;


}
