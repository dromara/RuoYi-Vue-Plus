package com.ruoyi.common.core.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree基类
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class TreeEntity<T> extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 父菜单名称
     */
    @TableField(exist = false)
    @Schema(name = "父菜单名称")
    private String parentName;

    /**
     * 父菜单ID
     */
    @Schema(name = "父菜单ID")
    private Long parentId;

    /**
     * 子部门
     */
    @TableField(exist = false)
    @Schema(name = "子部门")
    private List<T> children = new ArrayList<>();

}
