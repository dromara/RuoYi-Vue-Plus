package com.dromara.flow.ui.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 自定义user查询条件
 * Author: 土豆仙
 */
@Data
public class UserQueryBo {

    //用户ID   -对应账号
   /* protected String id;
    protected List<String> ids;
    protected String idIgnoreCase;*/

    private String userName;

    private List<String> userNames;

    //用户名称
    /*protected String firstName;
    protected String firstNameLike;
    protected String firstNameLikeIgnoreCase;
    protected String lastName;
    protected String lastNameLike;
    protected String lastNameLikeIgnoreCase;
    protected String fullNameLike;
    protected String fullNameLikeIgnoreCase;
    protected String displayName;
    protected String displayNameLike;
    protected String displayNameLikeIgnoreCase;*/

    private String nickName;

    //邮箱
    /*protected String email;
    protected String emailLike;*/

    private String email;

    private String emailLike;

    //用户组
    /*protected String groupId;
    protected List<String> groupIds;*/

    //约定：用户组转换成  区化、部门、角色  最后使用稳定的code值
    private List<GroupCodeBo> groupBoList;

    //租户
    protected String tenantId;
}
