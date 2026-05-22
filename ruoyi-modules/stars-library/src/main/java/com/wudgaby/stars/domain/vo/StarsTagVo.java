package com.wudgaby.stars.domain.vo;

import com.wudgaby.stars.domain.StarsTag;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户标签视图
 */
@Data
@AutoMapper(target = StarsTag.class)
public class StarsTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String color;

    private LocalDateTime createTime;

}
