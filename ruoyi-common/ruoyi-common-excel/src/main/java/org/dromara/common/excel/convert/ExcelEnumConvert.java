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
    private static final Map<Field, Map<Object, Object>> ENUM_REVERSE_MAP_CACHE = new ConcurrentHashMap<>();

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
        Map<Object, Object> enumTextToCodeMap = ENUM_REVERSE_MAP_CACHE.computeIfAbsent(
            contentProperty.getField(),
            f -> {
                Map<Object, Object> reverseMap = new HashMap<>();
                enumCodeToTextMap.forEach((key, value) -> reverseMap.put(value, key));
                return reverseMap;
            }
        );
        // 应该从text -> code中查找
        Object codeValue = enumTextToCodeMap.get(textValue);
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
                enumValueMap.put(codeValue, textValue);
            }
            return enumValueMap;
        });
    }

    private ExcelEnumFormat getAnnotation(Field field) {
        return AnnotationUtil.getAnnotation(field, ExcelEnumFormat.class);
    }
}
