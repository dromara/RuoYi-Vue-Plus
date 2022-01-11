package com.ruoyi.common.convert;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.convert.Convert;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.core.service.DictService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 字典格式化转换处理
 *
 * @author Lion Li
 */
@Slf4j
public class ExcelDictConvert implements Converter<Object> {

    @Override
    public Class<Object> supportJavaTypeKey() {
        return Object.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    @Override
    public Object convertToJavaData(CellData cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        ExcelDictFormat anno = getAnnotation(contentProperty.getField());
        String type = anno.dictType();
        String label = cellData.getStringValue();
        String value;
        if (StringUtils.isBlank(type)) {
            value = ExcelUtil.reverseByExp(label, anno.readConverterExp(), anno.separator());
        } else {
            value = SpringUtils.getBean(DictService.class).getDictValue(type, label, anno.separator());
        }
        return Convert.convert(contentProperty.getField().getType(), value);
    }

    @Override
    public CellData<String> convertToExcelData(Object object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (StringUtils.isNull(object)) {
            return new CellData<>("");
        }
        ExcelDictFormat anno = getAnnotation(contentProperty.getField());
        String type = anno.dictType();
        String value = Convert.toStr(object);
        String label;
        if (StringUtils.isBlank(type)) {
            label = ExcelUtil.convertByExp(value, anno.readConverterExp(), anno.separator());
        } else {
            label = SpringUtils.getBean(DictService.class).getDictLabel(type, value, anno.separator());
        }
        return new CellData<>(label);
    }

    private ExcelDictFormat getAnnotation(Field field) {
        return AnnotationUtil.getAnnotation(field, ExcelDictFormat.class);
    }
}
