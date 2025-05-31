package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsTenantSetting;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveStrategy;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户配置视图对象 pms_tenant_settings
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsTenantSetting.class)
public class PmsTenantSettingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @ExcelProperty(value = "配置ID")
    private Long settingId;

    /**
     * 租户ID
     */
    @ExcelProperty(value = "租户ID")
    private String tenantId;

    /**
     * 部门ID (门店)
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 门店名称
     */
    @ExcelProperty(value = "门店名称")
    @Translation(type = TransConstant.DEPT_ID_TO_NAME, mapper = "deptId")
    private String deptName;

    /**
     * 配置分组
     */
    @ExcelProperty(value = "配置分组")
    private String settingGroup;

    /**
     * 配置键
     */
    @ExcelProperty(value = "配置键")
    private String settingKey;

    /**
     * 配置值
     */
    @ExcelProperty(value = "配置值")
    @Sensitive(strategy = SensitiveStrategy.PASSWORD)
    private String settingValue;

    /**
     * 配置名称
     */
    @ExcelProperty(value = "配置名称")
    private String settingName;

    /**
     * 配置描述
     */
    @ExcelProperty(value = "配置描述")
    private String settingDescription;

    /**
     * 配置类型
     */
    @ExcelProperty(value = "配置类型")
    private String settingType;

    /**
     * 配置类型文本
     */
    @ExcelProperty(value = "配置类型")
    @Translation(type = TransConstant.DICT_TYPE_TO_LABEL, mapper = "pms_setting_type")
    private String settingTypeText;

    /**
     * 是否敏感配置
     */
    @ExcelProperty(value = "是否敏感配置")
    private Boolean isSensitive;

    /**
     * 是否可编辑
     */
    @ExcelProperty(value = "是否可编辑")
    private Boolean isEditable;

    /**
     * 是否系统配置
     */
    @ExcelProperty(value = "是否系统配置")
    private Boolean isSystem;

    /**
     * 排序值
     */
    @ExcelProperty(value = "排序值")
    private Integer sortOrder;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 状态文本
     */
    @ExcelProperty(value = "状态")
    @Translation(type = TransConstant.DICT_TYPE_TO_LABEL, mapper = "pms_setting_status")
    private String statusText;

    /**
     * 是否继承配置
     */
    @ExcelProperty(value = "是否继承配置")
    private Boolean isInherited;

    /**
     * 父级配置值
     */
    @ExcelProperty(value = "父级配置值")
    private String parentValue;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "updateBy")
    private String updateByName;
}
