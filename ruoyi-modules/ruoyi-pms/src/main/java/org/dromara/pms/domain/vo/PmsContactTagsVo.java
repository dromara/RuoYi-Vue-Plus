package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsContactTags;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;



/**
 * 联系人标签视图对象 pms_contact_tags
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsContactTags.class)
public class PmsContactTagsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签唯一ID
     */
    @ExcelProperty(value = "标签唯一ID")
    private Long tagId;

    /**
     * 部门ID (门店, 可空)
     */
    @ExcelProperty(value = "部门ID (门店, 可空)")
    private Long deptId;

    /**
     * 标签名称
     */
    @ExcelProperty(value = "标签名称")
    private String name;

    /**
     * 标签显示颜色
     */
    @ExcelProperty(value = "标签显示颜色")
    private String color;

    /**
     * 标签分类
     */
    @ExcelProperty(value = "标签分类")
    private String category;

    /**
     * 标签描述
     */
    @ExcelProperty(value = "标签描述")
    private String description;

    /**
     * 是否为系统预设标签
     */
    @ExcelProperty(value = "是否为系统预设标签")
    private Long isSystem;

    /**
     * 排序值
     */
    @ExcelProperty(value = "排序值")
    private Long sortOrder;


}
