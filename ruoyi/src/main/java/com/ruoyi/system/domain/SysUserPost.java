package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户和岗位关联 sys_user_post
 *
 * @author Lion Li
 */

@Data
@TableName("sys_user_post")
@ApiModel("用户和岗位关联")
public class SysUserPost {

    /**
     * 用户ID
     */
    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    /**
     * 岗位ID
     */
    @ApiModelProperty(value = "岗位ID")
    private Long postId;

}
