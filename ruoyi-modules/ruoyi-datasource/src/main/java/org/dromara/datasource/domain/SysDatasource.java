package org.dromara.datasource.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 动态数据源对象 sys_datasource
 *
 * @author ixyxj
 * @date 2024-05-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_datasource")
public class SysDatasource extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 数据源类型
     */
    private String dsType;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源url
     */
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
    private String driverClassName;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;


}
