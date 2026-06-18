package ${packageName}.domain.bo;

import ${packageName}.domain.${ClassName};
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import java.io.Serial;
import java.io.Serializable;
<#if hasBetween>
import java.util.HashMap;
import java.util.Map;
</#if>
import lombok.Data;
import jakarta.validation.constraints.*;
<#list importList as import>
import ${import};
</#list>

/**
 * ${functionName}业务对象 ${tableName}
 *
 * @author ${author}
 * @date ${datetime}
 */
@Data
@AutoMapper(target = ${ClassName}.class, reverseConvertGenerate = false)
public class ${ClassName}Bo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

<#list columns as column>
<#if !table.isSuperColumn(column.javaField) && (column.query || column.insert || column.edit)>
    /**
     * ${column.columnComment}
     */
<#if column.insert && column.edit>
<#assign Group = "AddGroup.class, EditGroup.class">
<#elseif column.insert>
<#assign Group = "AddGroup.class">
<#elseif column.edit>
<#assign Group = "EditGroup.class">
</#if>
<#if column.required>
<#if column.javaType == 'String'>
    @NotBlank(message = "${column.columnComment}不能为空", groups = { ${Group} })
<#else>
    @NotNull(message = "${column.columnComment}不能为空", groups = { ${Group} })
</#if>
</#if>
    private ${column.javaType} ${column.javaField};

</#if>
</#list>
<#if hasBetween>
    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();
</#if>

}

