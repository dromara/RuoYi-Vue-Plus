package org.dromara.pms.domain.vo;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.dromara.pms.domain.PmsCustomerContacts;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 客户联系人视图对象 pms_customer_contacts
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsCustomerContacts.class)
public class PmsCustomerContactsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 联系人唯一ID
     */
    @ExcelProperty(value = "联系人唯一ID")
    private Long contactId;

    /**
     * 联系人类型
     */
    @ExcelProperty(value = "联系人类型")
    private String contactType;

    /**
     * 联系人姓名
     */
    @ExcelProperty(value = "联系人姓名")
    private String fullName;

    /**
     * 主要联系电话
     */
    @ExcelProperty(value = "主要联系电话")
    private String phoneNumber;

    /**
     * 电子邮件地址
     */
    @ExcelProperty(value = "电子邮件地址")
    private String email;

    /**
     * 微信OpenID
     */
    @ExcelProperty(value = "微信OpenID")
    private String wechatOpenid;

    /**
     * 微信UnionID
     */
    @ExcelProperty(value = "微信UnionID")
    private String wechatUnionid;

    /**
     * 性别
     */
    @ExcelProperty(value = "性别")
    private String gender;

    /**
     * 出生日期
     */
    @ExcelProperty(value = "出生日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dateOfBirth;

    /**
     * 证件类型
     */
    @ExcelProperty(value = "证件类型")
    private String idType;

    /**
     * 证件号码 (加密存储)
     */
    @ExcelProperty(value = "证件号码 (加密存储)")
    private String idNumberEncrypted;

    /**
     * 国籍代码
     */
    @ExcelProperty(value = "国籍代码")
    private String nationalityCountryCode;

    /**
     * 地址-省份
     */
    @ExcelProperty(value = "地址-省份")
    private String addressProvince;

    /**
     * 地址-城市
     */
    @ExcelProperty(value = "地址-城市")
    private String addressCity;

    /**
     * 地址-区县
     */
    @ExcelProperty(value = "地址-区县")
    private String addressDistrict;

    /**
     * 地址-详细地址
     */
    @ExcelProperty(value = "地址-详细地址")
    private String addressDetail;

    /**
     * 邮政编码
     */
    @ExcelProperty(value = "邮政编码")
    private String postalCode;

    /**
     * 联系人状态
     */
    @ExcelProperty(value = "联系人状态")
    private String contactStatus;

    /**
     * 会员等级
     */
    @ExcelProperty(value = "会员等级")
    private String memberLevel;

    /**
     * 总入住次数
     */
    @ExcelProperty(value = "总入住次数")
    private Long totalStays;

    /**
     * 总消费金额
     */
    @ExcelProperty(value = "总消费金额")
    private Long totalAmount;

    /**
     * 最后入住日期
     */
    @ExcelProperty(value = "最后入住日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastStayDate;

    /**
     * 备注信息
     */
    @ExcelProperty(value = "备注信息")
    private String remarks;

    /**
     * 关联的标签列表
     */
    private List<PmsContactTagsVo> tags;

    /**
     * 标签ID列表（用于编辑时的数据绑定）
     */
    private List<Long> tagIds;

}
