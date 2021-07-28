package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试树表对象 test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("test_tree")
public class TestTree implements Serializable {

    private static final long serialVersionUID=1L;


    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 父id
     */
    private Long parentId;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 树节点名
     */
    private String treeName;

    /**
     * 版本
     */
    @Version
    private Long version;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
