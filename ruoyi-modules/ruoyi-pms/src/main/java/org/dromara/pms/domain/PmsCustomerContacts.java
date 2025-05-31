package org.dromara.pms.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 客户联系人对象 pms_customer_contacts
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_customer_contacts")
public class PmsCustomerContacts extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 联系人唯一ID
     */
    @TableId(value = "contact_id")
    private Long contactId;

    /**
     * 联系人类型
     */
    private String contactType;

    /**
     * 联系人姓名
     */
    private String fullName;

    /**
     * 主要联系电话
     */
    private String phoneNumber;

    /**
     * 电子邮件地址
     */
    private String email;

    /**
     * 微信OpenID
     */
    private String wechatOpenid;

    /**
     * 微信UnionID
     */
    private String wechatUnionid;

    /**
     * 性别
     */
    private String gender;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dateOfBirth;

    /**
     * 证件类型
     */
    private String idType;

    /**
     * 证件号码 (加密存储)
     */
    private String idNumberEncrypted;

    /**
     * 国籍代码
     */
    private String nationalityCountryCode;

    /**
     * 地址-省份
     */
    private String addressProvince;

    /**
     * 地址-城市
     */
    private String addressCity;

    /**
     * 地址-区县
     */
    private String addressDistrict;

    /**
     * 地址-详细地址
     */
    private String addressDetail;

    /**
     * 邮政编码
     */
    private String postalCode;

    /**
     * 联系人状态
     */
    private String contactStatus;

    /**
     * 会员等级
     */
    private String memberLevel;

    /**
     * 总入住次数
     */
    private Long totalStays;

    /**
     * 总消费金额
     */
    private Long totalAmount;

    /**
     * 最后入住日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastStayDate;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

}
