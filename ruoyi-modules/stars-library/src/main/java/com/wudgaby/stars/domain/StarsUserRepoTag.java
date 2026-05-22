package com.wudgaby.stars.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户仓库-标签关联 stars_user_repo_tag
 */
@Data
@TableName("stars_user_repo_tag")
public class StarsUserRepoTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户仓库关系 ID
     */
    @TableId(type = IdType.INPUT)
    private Long userRepoId;

    /**
     * 标签 ID
     */
    private Long tagId;

}
