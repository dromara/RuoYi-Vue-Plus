package com.wudgaby.stars.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * GitHub 账号绑定 stars_github_account
 */
@Data
@TableName("stars_github_account")
public class StarsGithubAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * RuoYi 用户 ID
     */
    private Long userId;

    /**
     * GitHub 用户名
     */
    private String githubLogin;

    /**
     * AES 加密 PAT
     */
    private String accessToken;

    /**
     * Token 授权范围
     */
    private String tokenScope;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
