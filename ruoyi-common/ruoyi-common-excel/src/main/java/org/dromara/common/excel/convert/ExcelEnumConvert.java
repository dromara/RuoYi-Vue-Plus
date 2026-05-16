package org.dromara.common.excel.convert;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举格式化转换处理
 *
 * @author Liang
 */
@Slf4j
public class ExcelEnumConvert implements Converter<Object> {

    private static final Map<Field, Map<Object, String>> ENUM_MAP_CACHE = new ConcurrentHashMap<>();
    private static final Map<Field, Map<String, Object>> ENUM_REVERSE_MAP_CACHE = new ConcurrentHashMap<>();

    @Override
    public Class<Object> supportJavaTypeKey() {
        return Object.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        cellData.checkEmpty();
        // Excel中填入的是枚举中指定的描述
        Object textValue = switch (cellData.getType()) {
            case STRING, DIRECT_STRING, RICH_TEXT_STRING -> cellData.getStringValue();
            case NUMBER -> cellData.getNumberValue();
            case BOOLEAN -> cellData.getBooleanValue();
            default -> throw new IllegalArgumentException("单元格类型异常!");
        };
        // 如果是空值
        if (ObjectUtil.isNull(textValue)) {
            return null;
        }
        Map<Object, String> enumCodeToTextMap = beforeConvert(contentProperty);
        // 从Java输出至Excel是code转text，从Excel转Java应将text与code对调
        Map<String, Object> enumTextToCodeMap = ENUM_REVERSE_MAP_CACHE.computeIfAbsent(
            contentProperty.getField(),
            f -> {
                Map<String, Object> reverseMap = new HashMap<>();
                enumCodeToTextMap.forEach((key, value) -> {
                    Object oldValue = reverseMap.put(value, key);
                    if (ObjectUtil.isNotNull(oldValue)) {
                        throw new IllegalArgumentException("枚举导入文本值重复: " + value);
                    }
                });
                return reverseMap;
            }
        );
        // 应该从text -> code中查找
        Object codeValue = enumTextToCodeMap.get(Convert.toStr(textValue));
        if (ObjectUtil.isNull(codeValue)) {
            throw new IllegalArgumentException("枚举值不匹配: " + textValue + "，允许值: " + enumTextToCodeMap.keySet());
        }
        return Convert.convert(contentProperty.getField().getType(), codeValue);
    }

    @Override
    public WriteCellData<String> convertToExcelData(Object object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (ObjectUtil.isNull(object)) {
            return new WriteCellData<>("");
        }
        Map<Object, String> enumValueMap = beforeConvert(contentProperty);
        String value = Convert.toStr(enumValueMap.get(object), "");
        return new WriteCellData<>(value);
    }

    private Map<Object, String> beforeConvert(ExcelContentProperty contentProperty) {
        return ENUM_MAP_CACHE.computeIfAbsent(contentProperty.getField(), field -> {
            ExcelEnumFormat anno = getAnnotation(field);
            Map<Object, String> enumValueMap = new HashMap<>();
            Enum<?>[] enumConstants = anno.enumClass().getEnumConstants();
            for (Enum<?> enumConstant : enumConstants) {
                Object codeValue = ReflectUtils.invokeGetter(enumConstant, anno.codeField());
                String textValue = ReflectUtils.invokeGetter(enumConstant, anno.textField());
                if (ObjectUtil.isNull(codeValue) || ObjectUtil.isNull(textValue)) {
                    throw new IllegalArgumentException("枚举字段 code/text 不能为空: " + enumConstant.name());
                }
                enumValueMap.put(codeValue, textValue);
            }
            return enumValueMap;
        });
    }

    private ExcelEnumFormat getAnnotation(Field field) {
        return AnnotationUtil.getAnnotation(field, ExcelEnumFormat.class);
    }
}
