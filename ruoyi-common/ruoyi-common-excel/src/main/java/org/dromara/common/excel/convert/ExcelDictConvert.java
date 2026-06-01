package org.dromara.common.excel.convert;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.excel.annotation.ExcelDictFormat;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 字典格式化转换处理
 *
 * @author Lion Li
 */
@Slf4j
public class ExcelDictConvert implements Converter<Object> {

    private DictService dictService;

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
     * 将 Excel 字典显示值转换为 Java 字段值。
     *
     * @param cellData            单元格数据
     * @param contentProperty     内容属性
     * @param globalConfiguration 全局配置
     * @return Java 字段值
     */
    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        ExcelDictFormat anno = getAnnotation(contentProperty.getField());
        String type = anno.dictType();
        String label = cellData.getStringValue();
        String value;
        if (StringUtils.isBlank(type)) {
            value = reverseByExp(label, anno.readConverterExp(), anno.separator());
        } else {
            value = getDictService().getDictValue(type, label, anno.separator());
        }
        return Convert.convert(contentProperty.getField().getType(), value);
    }

    /**
     * 将 Java 字段值转换为 Excel 字典显示值。
     *
     * @param object              Java 字段值
     * @param contentProperty     内容属性
     * @param globalConfiguration 全局配置
     * @return Excel 写入数据
     */
    @Override
    public WriteCellData<String> convertToExcelData(Object object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (ObjectUtil.isNull(object)) {
            return new WriteCellData<>("");
        }
        ExcelDictFormat anno = getAnnotation(contentProperty.getField());
        String type = anno.dictType();
        String value = Convert.toStr(object);
        String label;
        if (StringUtils.isBlank(type)) {
            label = convertByExp(value, anno.readConverterExp(), anno.separator());
        } else {
            label = getDictService().getDictLabel(type, value, anno.separator());
        }
        return new WriteCellData<>(label);
    }

    /**
     * 获取字段上的字典格式注解。
     *
     * @param field 字段
     * @return 字典格式注解
     */
    private ExcelDictFormat getAnnotation(Field field) {
        return AnnotationUtil.getAnnotation(field, ExcelDictFormat.class);
    }

    /**
     * 延迟获取字典服务。
     *
     * @return 字典服务
     */
    private DictService getDictService() {
        if (dictService == null) {
            dictService = SpringUtils.getBean(DictService.class);
        }
        return dictService;
    }

    /**
     * 解析导出值 0=男,1=女,2=未知。
     *
     * @param propertyValue 字段值
     * @param converterExp  转换表达式
     * @param separator     分隔符
     * @return 导出显示值
     */
    private static String convertByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        Map<String, String> convertSource = parseConverterExp(converterExp);
        for (Map.Entry<String, String> item : convertSource.entrySet()) {
            if (StringUtils.contains(propertyValue, separator)) {
                for (String value : splitPropertyValue(propertyValue, separator)) {
                    if (item.getKey().equals(value)) {
                        propertyString.append(item.getValue()).append(separator);
                        break;
                    }
                }
            } else {
                if (item.getKey().equals(propertyValue)) {
                    return item.getValue();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 反向解析值 男=0,女=1,未知=2。
     *
     * @param propertyValue 显示值
     * @param converterExp  转换表达式
     * @param separator     分隔符
     * @return 字段值
     */
    private static String reverseByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        Map<String, String> convertSource = parseConverterExp(converterExp);
        for (Map.Entry<String, String> item : convertSource.entrySet()) {
            if (StringUtils.contains(propertyValue, separator)) {
                for (String value : splitPropertyValue(propertyValue, separator)) {
                    if (item.getValue().equals(value)) {
                        propertyString.append(item.getKey()).append(separator);
                        break;
                    }
                }
            } else {
                if (item.getValue().equals(propertyValue)) {
                    return item.getKey();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 解析转换表达式。
     *
     * @param converterExp 转换表达式
     * @return 字段值与显示值映射
     */
    private static Map<String, String> parseConverterExp(String converterExp) {
        Map<String, String> result = new LinkedHashMap<>();
        if (StringUtils.isBlank(converterExp)) {
            return result;
        }
        for (String item : converterExp.split(StringUtils.SEPARATOR)) {
            String[] itemArray = item.split("=", 2);
            if (itemArray.length != 2) {
                throw new IllegalArgumentException("Excel转换表达式格式错误: " + item);
            }
            result.put(itemArray[0], itemArray[1]);
        }
        return result;
    }

    /**
     * 按分隔符拆分多值字段。
     *
     * @param propertyValue 字段值
     * @param separator     分隔符
     * @return 拆分后的字段值数组
     */
    private static String[] splitPropertyValue(String propertyValue, String separator) {
        return propertyValue.split(Pattern.quote(separator));
    }
}
