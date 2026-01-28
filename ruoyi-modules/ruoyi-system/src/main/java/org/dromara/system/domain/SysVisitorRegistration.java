package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 访客预约登记表 sys_visitor_registration
 *
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_visitor_registration")
public class SysVisitorRegistration extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 访客姓名
     */
    private String visitorName;

    /**
     * 访客联系电话
     */
    private String visitorPhone;

    /**
     * 访问事由
     */
    private String visitPurpose;

    /**
     * 预约访问部门ID
     */
    private Long deptId;

    /**
     * 预约到访时间
     */
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
    private String remark;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 部门名称
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 状态名称
     */
    @TableField(exist = false)
    private String statusName;

    /**
     * 状态常量
     */
    public static final String STATUS_APPOINTMENT = "0";
    public static final String STATUS_CHECKED_IN = "1";
    public static final String STATUS_CHECKED_OUT = "2";
    public static final String STATUS_CANCELLED = "3";

    /**
     * 状态名称映射
     */
    public String getStatusName() {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case STATUS_APPOINTMENT -> "预约中";
            case STATUS_CHECKED_IN -> "已签到";
            case STATUS_CHECKED_OUT -> "已签离";
            case STATUS_CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}