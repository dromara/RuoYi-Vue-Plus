package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysVisitorRegistration;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 访客预约登记视图对象 sys_visitor_registration
 *
 * @author System
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysVisitorRegistration.class)
public class SysVisitorRegistrationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 访客姓名
     */
    @ExcelProperty(value = "访客姓名")
    private String visitorName;

    /**
     * 访客联系电话
     */
    @ExcelProperty(value = "访客联系电话")
    private String visitorPhone;

    /**
     * 访问事由
     */
    @ExcelProperty(value = "访问事由")
    private String visitPurpose;

    /**
     * 预约访问部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty(value = "访问部门")
    private String deptName;

    /**
     * 预约到访时间
     */
    @ExcelProperty(value = "预约到访时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date appointmentTime;

    /**
     * 状态（0预约中 1已签到 2已签离 3已取消）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 状态名称
     */
    @ExcelProperty(value = "状态描述")
    private String statusName;

    /**
     * 实际签到时间
     */
    @ExcelProperty(value = "签到时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date checkInTime;

    /**
     * 实际签离时间
     */
    @ExcelProperty(value = "签离时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date checkOutTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;

}