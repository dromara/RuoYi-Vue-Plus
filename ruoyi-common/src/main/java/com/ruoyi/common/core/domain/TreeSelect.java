package com.ruoyi.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysMenu;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Treeselect树结构实体类
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("树结构实体类")
public class TreeSelect implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    @ApiModelProperty(value = "节点ID")
    private Long id;

    /**
     * 节点名称
     */
    @ApiModelProperty(value = "节点名称")
    private String label;

    /**
     * 子节点
     */
    @ApiModelProperty(value = "子节点")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TreeSelect> children;

    public TreeSelect(SysDept dept) {
        this.id = dept.getDeptId();
        this.label = dept.getDeptName();
        this.children = dept.getChildren()
                .stream()
                .map(d -> new TreeSelect((SysDept) d))
                .collect(Collectors.toList());
    }

    public TreeSelect(SysMenu menu) {
        this.id = menu.getMenuId();
        this.label = menu.getMenuName();
        this.children = menu.getChildren()
                .stream()
                .map(d -> new TreeSelect((SysMenu) d))
                .collect(Collectors.toList());
    }

}
