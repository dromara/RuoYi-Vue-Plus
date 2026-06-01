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

    /**
     * 枚举字典缓存。
     */
    private static final Map<Field, Map<Object, String>> ENUM_MAP_CACHE = new ConcurrentHashMap<>();
    /**
     * 枚举反向字典缓存。
     */
    private static final Map<Field, Map<String, Object>> ENUM_REVERSE_MAP_CACHE = new ConcurrentHashMap<>();

    /**
     * 支持的 Java 类型。
     *
     * @return Object 类型
     */
    @Override
    public Class<Object> supportJavaTypeKey() {
        return Object.class;
    }

    /**
     * 支持的 Excel 单元格类型。
     *
     * @return 默认支持全部类型
     */
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    /**
     * 将 Excel 枚举文本转换为 Java 字段值。
     *
     * @param cellData 单元格数据
     * @param contentProperty 内容属性
     * @param globalConfiguration 全局配置
     * @return Java 字段值
     */
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

    /**
     * 将 Java 枚举编码转换为 Excel 显示文本。
     *
     * @param object Java 字段值
     * @param contentProperty 内容属性
     * @param globalConfiguration 全局配置
     * @return Excel 写入数据
     */
    @Override
    public WriteCellData<String> convertToExcelData(Object object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (ObjectUtil.isNull(object)) {
            return new WriteCellData<>("");
        }
        Map<Object, String> enumValueMap = beforeConvert(contentProperty);
        String value = Convert.toStr(enumValueMap.get(object), "");
        return new WriteCellData<>(value);
    }

    /**
     * 准备枚举编码与显示文本映射。
     *
     * @param contentProperty 内容属性
     * @return 枚举编码与显示文本映射
     */
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

    /**
     * 获取字段上的枚举格式注解。
     *
     * @param field 字段
     * @return 枚举格式注解
     */
    private ExcelEnumFormat getAnnotation(Field field) {
        return AnnotationUtil.getAnnotation(field, ExcelEnumFormat.class);
    }
}
