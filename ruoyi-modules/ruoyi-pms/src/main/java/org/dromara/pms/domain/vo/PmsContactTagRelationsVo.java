package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsContactTagRelations;
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
 * 联系人标签关联视图对象 pms_contact_tag_relations
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsContactTagRelations.class)
public class PmsContactTagRelationsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联唯一ID
     */
    @ExcelProperty(value = "关联唯一ID")
    private Long relationId;

    /**
     * 联系人ID
     */
    @ExcelProperty(value = "联系人ID")
    private Long contactId;

    /**
     * 标签ID
     */
    @ExcelProperty(value = "标签ID")
    private Long tagId;

    /**
     * 联系人姓名（关联查询）
     */
    private String contactName;

    /**
     * 标签名称（关联查询）
     */
    private String tagName;

    /**
     * 标签颜色（关联查询）
     */
    private String tagColor;

}
