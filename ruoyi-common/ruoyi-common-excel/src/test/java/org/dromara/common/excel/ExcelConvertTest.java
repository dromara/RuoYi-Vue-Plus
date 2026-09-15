package org.dromara.common.excel;

import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.convert.ExcelBigNumberConvert;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.common.excel.convert.ExcelEnumConvert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Excel 转换器单元测试")
class ExcelConvertTest {

    /**
     * 验证普通 Long 按数字写出，而超过 Excel 精度上限的 Long 按字符串写出。
     */
    @Test
    @DisplayName("按精度范围写出 Long")
    void shouldWriteLongAccordingToExcelPrecisionLimit() {
        ExcelBigNumberConvert converter = new ExcelBigNumberConvert();

        WriteCellData<Object> normal = converter.convertToExcelData(123456789012345L, null, null);
        WriteCellData<Object> large = converter.convertToExcelData(1234567890123456L, null, null);
        WriteCellData<Object> empty = converter.convertToExcelData(null, null, null);

        assertEquals(CellDataTypeEnum.NUMBER, normal.getType());
        assertEquals(new BigDecimal("123456789012345"), normal.getNumberValue());
        assertEquals("1234567890123456", large.getStringValue());
        assertEquals("", empty.getStringValue());
        assertEquals(42L, converter.convertToJavaData(new ReadCellData<>("42"), null, null));
    }

    /**
     * 验证字典表达式支持单值和多值的双向转换，并保留目标字段类型。
     */
    @Test
    @DisplayName("双向转换字典表达式")
    void shouldConvertDictionaryExpressionInBothDirections() throws Exception {
        ExcelDictConvert converter = new ExcelDictConvert();
        ExcelContentProperty singleProperty = property("status");
        ExcelContentProperty multiProperty = property("roles");

        assertEquals("启用", converter.convertToExcelData(1, singleProperty, null).getStringValue());
        assertEquals(0, converter.convertToJavaData(new ReadCellData<>("停用"), singleProperty, null));
        assertEquals("管理员|访客", converter.convertToExcelData("A|G", multiProperty, null).getStringValue());
        assertEquals("A|G", converter.convertToJavaData(new ReadCellData<>("访客|管理员"), multiProperty, null));
        assertEquals("", converter.convertToExcelData(null, singleProperty, null).getStringValue());
    }

    /**
     * 验证格式错误的字典表达式会被明确拒绝，避免静默生成错误导入导出值。
     */
    @Test
    @DisplayName("拒绝格式错误的字典表达式")
    void shouldRejectMalformedDictionaryExpression() throws Exception {
        ExcelDictConvert converter = new ExcelDictConvert();

        assertThrows(IllegalArgumentException.class,
            () -> converter.convertToExcelData("1", property("malformed"), null));
    }

    /**
     * 验证枚举编码和显示文本可以双向转换，未知显示文本会返回可诊断异常。
     */
    @Test
    @DisplayName("双向转换枚举编码与文本")
    void shouldConvertEnumCodeAndTextInBothDirections() throws Exception {
        ExcelEnumConvert converter = new ExcelEnumConvert();
        ExcelContentProperty property = property("level");

        assertEquals("高级", converter.convertToExcelData(2, property, null).getStringValue());
        assertEquals(1, converter.convertToJavaData(new ReadCellData<>("普通"), property, null));
        assertEquals("", converter.convertToExcelData(null, property, null).getStringValue());
        assertThrows(IllegalArgumentException.class,
            () -> converter.convertToJavaData(new ReadCellData<>("未知级别"), property, null));
    }

    /**
     * 创建绑定指定测试字段的 Excel 内容属性。
     *
     * @param fieldName 测试字段名
     * @return Excel 内容属性
     */
    private static ExcelContentProperty property(String fieldName) throws NoSuchFieldException {
        Field field = ExcelRow.class.getDeclaredField(fieldName);
        ExcelContentProperty property = new ExcelContentProperty();
        property.setField(field);
        return property;
    }

    private static class ExcelRow {

        @ExcelDictFormat(readConverterExp = "0=停用,1=启用")
        private Integer status;

        @ExcelDictFormat(readConverterExp = "A=管理员,G=访客", separator = "|")
        private String roles;

        @ExcelDictFormat(readConverterExp = "0=正常,错误项")
        private String malformed;

        @ExcelEnumFormat(enumClass = Level.class)
        private Integer level;
    }

    private enum Level {
        NORMAL(1, "普通"),
        HIGH(2, "高级");

        private final int code;
        private final String text;

        Level(int code, String text) {
            this.code = code;
            this.text = text;
        }

        /**
         * 返回枚举编码供转换器反射读取。
         *
         * @return 枚举编码
         */
        public int getCode() {
            return code;
        }

        /**
         * 返回枚举显示文本供转换器反射读取。
         *
         * @return 显示文本
         */
        public String getText() {
            return text;
        }
    }
}
