package org.dromara.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.workflow.domain.WfCategory;

import java.io.Serial;
import java.io.Serializable;


/**
 * 流程分类视图对象 wf_category
 *
 * @author may
 * @date 2023-06-27
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WfCategory.class)
public class WfCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 分类名称
     */
    @ExcelProperty(value = "分类名称")
    private String categoryName;

    /**
     * 分类编码
     */
    @ExcelProperty(value = "分类编码")
    private String categoryCode;

    /**
     * 父级id
     */
    @ExcelProperty(value = "父级id")
    private Long parentId;

    /**
     * 排序
     */
    @ExcelProperty(value = "排序")
    private Long sortNum;


}
