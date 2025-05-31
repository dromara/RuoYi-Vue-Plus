package org.dromara.pms.domain.bo;

import org.dromara.pms.domain.PmsCustomerContacts;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 客户联系人业务对象 pms_customer_contacts
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsCustomerContacts.class, reverseConvertGenerate = false)
public class PmsCustomerContactsBo extends BaseEntity {

    /**
     * 联系人唯一ID
     */
    @NotNull(message = "联系人唯一ID不能为空", groups = { EditGroup.class })
    private Long contactId;

    /**
     * 联系人类型
     */
    @NotBlank(message = "联系人类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String contactType;

    /**
     * 联系人姓名
     */
    @NotBlank(message = "联系人姓名不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 255, message = "联系人姓名长度不能超过255个字符")
    private String fullName;

    /**
     * 主要联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Size(max = 50, message = "电话号码长度不能超过50个字符")
    private String phoneNumber;

    /**
     * 电子邮件地址
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱地址长度不能超过255个字符")
    private String email;

    /**
     * 微信OpenID
     */
    @Size(max = 100, message = "微信OpenID长度不能超过100个字符")
    private String wechatOpenid;

    /**
     * 微信UnionID
     */
    @Size(max = 100, message = "微信UnionID长度不能超过100个字符")
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
    @Size(max = 10, message = "国籍代码长度不能超过10个字符")
    private String nationalityCountryCode;

    /**
     * 地址-省份
     */
    @Size(max = 100, message = "省份名称长度不能超过100个字符")
    private String addressProvince;

    /**
     * 地址-城市
     */
    @Size(max = 100, message = "城市名称长度不能超过100个字符")
    private String addressCity;

    /**
     * 地址-区县
     */
    @Size(max = 100, message = "区县名称长度不能超过100个字符")
    private String addressDistrict;

    /**
     * 地址-详细地址
     */
    @Size(max = 500, message = "详细地址长度不能超过500个字符")
    private String addressDetail;

    /**
     * 邮政编码
     */
    @Pattern(regexp = "^\\d{6}$", message = "邮政编码格式不正确")
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
    @Min(value = 0, message = "总入住次数不能为负数")
    private Integer totalStays;

    /**
     * 总消费金额
     */
    @DecimalMin(value = "0.00", message = "总消费金额不能为负数")
    private java.math.BigDecimal totalAmount;

    /**
     * 最后入住日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastStayDate;

    /**
     * 备注信息
     */
    @Size(max = 1000, message = "备注信息长度不能超过1000个字符")
    private String remarks;

}
