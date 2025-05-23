package org.dromara.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.workflow.domain.FlowCategory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 流程分类视图对象 wf_category
 *
 * @author may
 * @date 2023-06-27
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FlowCategory.class)
public class FlowCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程分类ID
     */
    @ExcelProperty(value = "流程分类ID")
    private Long categoryId;

    /**
     * 父级id
     */
    private Long parentId;

    /**
     * 父类别名称
     */
    private String parentName;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 流程分类名称
     */
    @ExcelProperty(value = "流程分类名称")
    private String categoryName;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = "显示顺序")
    private Long orderNum;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 子菜单
     */
    private List<FlowCategoryVo> children = new ArrayList<>();

}
