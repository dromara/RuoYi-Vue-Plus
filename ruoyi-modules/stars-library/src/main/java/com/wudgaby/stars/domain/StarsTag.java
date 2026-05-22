package com.wudgaby.stars.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户标签 stars_tag
 */
@Data
@TableName("stars_tag")
public class StarsTag implements Serializable {

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
     * 标签名
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
