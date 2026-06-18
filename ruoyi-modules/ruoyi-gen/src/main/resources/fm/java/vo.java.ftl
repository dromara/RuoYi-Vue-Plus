package ${packageName}.domain.vo;

<#list importList as import>
import ${import};
</#list>
import ${packageName}.domain.${ClassName};
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;



/**
 * ${functionName}视图对象 ${tableName}
 *
 * @author ${author}
 * @date ${datetime}
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ${ClassName}.class)
public class ${ClassName}Vo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

<#list columns as column>
<#if column.list>
    /**
     * ${column.columnComment}
     */
<#assign parentheseIndex = column.columnComment?index_of("（")>
<#if column.dictType?has_content>
    @ExcelProperty(value = "${column.columnLabel}", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "${column.dictType}")
<#elseif parentheseIndex != -1>
    @ExcelProperty(value = "${column.columnLabel}", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "${column.readConverterExp}()")
<#else>
    @ExcelProperty(value = "${column.columnLabel}")
</#if>
    private ${column.javaType} ${column.javaField};

<#if column.htmlType == "imageUpload">
    /**
     * ${column.columnComment}Url
     */
    @Translation(type = TransConstant.OSS_ID_TO_URL, mapper = "${column.javaField}")
    private String ${column.javaField}Url;
</#if>
</#if>
</#list>

}


