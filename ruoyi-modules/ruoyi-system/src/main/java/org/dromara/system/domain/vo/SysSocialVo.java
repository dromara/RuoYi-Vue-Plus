package org.dromara.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysSocial;

import java.io.Serial;
import java.io.Serializable;


/**
 * 社会化关系视图对象 sys_social
 *
 * @author thiszhc
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysSocial.class)
public class SysSocialVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 的唯一ID
     */
    @ExcelProperty(value = "授权UUID")
    private String authId;

    /**
     * 用户来源
     */
    @ExcelProperty(value = "用户来源")
    private String source;

    /**
     * 用户的授权令牌
     */
    @ExcelProperty(value = "用户的授权令牌")
    private String accessToken;

    /**
     * 用户的授权令牌的有效期，部分平台可能没有
     */
    @ExcelProperty(value = "用户的授权令牌的有效期，部分平台可能没有")
    private int expireIn;

    /**
     * 刷新令牌，部分平台可能没有
     */
    @ExcelProperty(value = "刷新令牌，部分平台可能没有")
    private String refreshToken;

    /**
     * 用户的 open id
     */
    @ExcelProperty(value = "用户的 open id")
    private String openId;

    /**
     * 授权的第三方账号
     */
    @ExcelProperty(value = "授权的第三方账号")
    private String userName;

    /**
     * 授权的第三方昵称
     */
    @ExcelProperty(value = "授权的第三方昵称")
    private String nickName;

    /**
     * 授权的第三方邮箱
     */
    @ExcelProperty(value = "授权的第三方邮箱")
    private String email;

    /**
     * 授权的第三方头像地址
     */
    @ExcelProperty(value = "授权的第三方头像地址")
    private String avatar;


    /**
     * 平台的授权信息，部分平台可能没有
     */
    @ExcelProperty(value = "平台的授权信息，部分平台可能没有")
    private String accessCode;

    /**
     * 用户的 unionid
     */
    @ExcelProperty(value = "用户的 unionid")
    private String unionId;

    /**
     * 授予的权限，部分平台可能没有
     */
    @ExcelProperty(value = "授予的权限，部分平台可能没有")
    private String scope;

    /**
     * 个别平台的授权信息，部分平台可能没有
     */
    @ExcelProperty(value = "个别平台的授权信息，部分平台可能没有")
    private String tokenType;

    /**
     * id token，部分平台可能没有
     */
    @ExcelProperty(value = "id token，部分平台可能没有")
    private String idToken;

    /**
     * 小米平台用户的附带属性，部分平台可能没有
     */
    @ExcelProperty(value = "小米平台用户的附带属性，部分平台可能没有")
    private String macAlgorithm;

    /**
     * 小米平台用户的附带属性，部分平台可能没有
     */
    @ExcelProperty(value = "小米平台用户的附带属性，部分平台可能没有")
    private String macKey;

    /**
     * 用户的授权code，部分平台可能没有
     */
    @ExcelProperty(value = "用户的授权code，部分平台可能没有")
    private String code;

    /**
     * Twitter平台用户的附带属性，部分平台可能没有
     */
    @ExcelProperty(value = "Twitter平台用户的附带属性，部分平台可能没有")
    private String oauthToken;

    /**
     * Twitter平台用户的附带属性，部分平台可能没有
     */
    @ExcelProperty(value = "Twitter平台用户的附带属性，部分平台可能没有")
    private String oauthTokenSecret;


}
