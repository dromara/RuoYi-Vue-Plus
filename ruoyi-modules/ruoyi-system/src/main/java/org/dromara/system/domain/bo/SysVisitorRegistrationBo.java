package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysVisitorRegistration;

import java.util.Date;

/**
 * 访客预约登记业务对象 sys_visitor_registration
 *
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysVisitorRegistration.class, reverseConvertGenerate = false)
public class SysVisitorRegistrationBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 访客姓名
     */
    @NotBlank(message = "访客姓名不能为空")
    @Size(min = 1, max = 50, message = "访客姓名长度不能超过{max}个字符")
    private String visitorName;

    /**
     * 访客联系电话
     */
    @NotBlank(message = "访客联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$|^0\\d{2,3}-?\\d{7,8}$", message = "联系电话格式不正确")
    private String visitorPhone;

    /**
     * 访问事由
     */
    @NotBlank(message = "访问事由不能为空")
    @Size(min = 1, max = 500, message = "访问事由长度不能超过{max}个字符")
    private String visitPurpose;

    /**
     * 预约访问部门ID
     */
    @NotNull(message = "预约访问部门不能为空")
    private Long deptId;

    /**
     * 预约到访时间
     */
    @NotNull(message = "预约到访时间不能为空")
    private Date appointmentTime;

    /**
     * 状态（0预约中 1已签到 2已签离 3已取消）
     */
    private String status;

    /**
     * 实际签到时间
     */
    private Date checkInTime;

    /**
     * 实际签离时间
     */
    private Date checkOutTime;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

    /**
     * 访客姓名查询关键字
     */
    private String visitorNameKeyword;

    /**
     * 预约时间范围查询 - 开始时间
     */
    private Date appointmentTimeStart;

    /**
     * 预约时间范围查询 - 结束时间
     */
    private Date appointmentTimeEnd;

    /**
     * 状态列表查询
     */
    private String[] statusList;

}