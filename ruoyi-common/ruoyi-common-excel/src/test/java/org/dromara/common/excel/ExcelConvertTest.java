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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Excel 转换器单元测试")
class ExcelConvertTest {

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "-9223372036854775809", "18.75", "-0.25"})
    @DisplayName("无法精确表示为 Long 的数值单元格触发转换错误")
    void shouldRejectLossyLongConversion(String value) {
        var converter = new ExcelBigNumberConvert();
        var cell = new ReadCellData<>(new BigDecimal(value));
        assertThrows(ArithmeticException.class, () -> converter.convertToJavaData(cell, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "-9223372036854775809", "18.75", "-0.25"})
    @DisplayName("文本存储的大数也必须能精确转换为 Long")
    void shouldRejectLossyTextLongConversion(String value) {
        var converter = new ExcelBigNumberConvert();
        assertThrows(ArithmeticException.class, () -> converter.convertToJavaData(new ReadCellData<>(value), null, null));
    }

    @Test
    @DisplayName("数值型枚举标签可以按显示文本导出并从数字单元格导入")
    void shouldRoundTripNumericEnumLabel() throws Exception {
        var converter = new ExcelEnumConvert();
        var property = property("numericLabel");
        assertEquals("2", converter.convertToExcelData("高级", property, null).getStringValue());
        assertEquals("高级", converter.convertToJavaData(new ReadCellData<>(BigDecimal.valueOf(2)), property, null));
    }

    @Test
    @DisplayName("可选枚举字段的空单元格导入为空值")
    void shouldReadEmptyEnumCell() throws Exception {
        var converter = new ExcelEnumConvert();
        assertNull(converter.convertToJavaData(new ReadCellData<>(CellDataTypeEnum.EMPTY), property("level"), null));
    }

    @Test
    @DisplayName("整数枚举编码导入字符串字段后仍能导出原标签")
    void shouldRoundTripEnumCodeThroughStringField() throws Exception {
        ExcelEnumConvert converter = new ExcelEnumConvert();
        ExcelContentProperty property = property("levelCode");

        Object code = converter.convertToJavaData(new ReadCellData<>("高级"), property, null);
        assertEquals("2", code);
        assertEquals("高级", converter.convertToExcelData(code, property, null).getStringValue());
        assertEquals("", converter.convertToExcelData("02", property, null).getStringValue());
    }

    @Test
    @DisplayName("数值单元格按字典标签查找编码")
    void shouldResolveNumericDictionaryLabel() throws Exception {
        ExcelDictConvert converter = new ExcelDictConvert();
        ReadCellData<?> cell = new ReadCellData<>(BigDecimal.TEN);
        assertEquals("TEN", converter.convertToJavaData(cell, property("typedLabel"), null));
    }

    @Test
    @DisplayName("布尔单元格按字典标签查找编码")
    void shouldResolveBooleanDictionaryLabel() throws Exception {
        ExcelDictConvert converter = new ExcelDictConvert();
        ReadCellData<?> cell = new ReadCellData<>(false);
        assertEquals("OFF", converter.convertToJavaData(cell, property("typedLabel"), null));
    }

    @Test
    @DisplayName("多选字典导出保留标签末尾的单个冒号")
    void shouldPreserveLabelSuffixOnExport() throws Exception {
        ExcelDictConvert converter = new ExcelDictConvert();
        assertEquals("普通::备注:", converter.convertToExcelData("A::B:", property("categories"), null).getStringValue());
    }

    @Test
    @DisplayName("多选字典导入保留编码末尾的单个冒号")
    void shouldPreserveCodeSuffixOnImport() throws Exception {
        ExcelDictConvert converter = new ExcelDictConvert();
        assertEquals("A::B:", converter.convertToJavaData(new ReadCellData<>("普通::备注:"), property("categories"), null));
    }

    @Test
    @DisplayName("数值单元格导入 Long 时保留整数值")
    void shouldReadLongFromNumericCell() {
        ExcelBigNumberConvert converter = new ExcelBigNumberConvert();
        ReadCellData<?> cell = new ReadCellData<>(new BigDecimal("123456789012345"));
        assertEquals(123456789012345L, converter.convertToJavaData(cell, null, null));
    }

    @Test
    @DisplayName("负号不计入 Excel 的 15 位数字精度")
    void shouldWriteNegativeLongAccordingToPrecision() {
        ExcelBigNumberConvert converter = new ExcelBigNumberConvert();
        WriteCellData<Object> cell = converter.convertToExcelData(-123456789012345L, null, null);
        assertEquals(CellDataTypeEnum.NUMBER, cell.getType());
        assertEquals(new BigDecimal("-123456789012345"), cell.getNumberValue());
        assertEquals("-9223372036854775808", converter.convertToExcelData(Long.MIN_VALUE, null, null).getStringValue());
    }

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

        @ExcelEnumFormat(enumClass = Level.class, codeField = "text", textField = "code")
        private String numericLabel;

        @ExcelDictFormat(readConverterExp = "TEN=10,OFF=false")
        private String typedLabel;

        @ExcelDictFormat(readConverterExp = "0=停用,1=启用")
        private Integer status;

        @ExcelDictFormat(readConverterExp = "A=管理员,G=访客", separator = "|")
        private String roles;

        @ExcelDictFormat(readConverterExp = "A=普通,B:=备注:", separator = "::")
        private String categories;

        @ExcelDictFormat(readConverterExp = "0=正常,错误项")
        private String malformed;

        @ExcelEnumFormat(enumClass = Level.class)
        private Integer level;

        @ExcelEnumFormat(enumClass = Level.class)
        private String levelCode;
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
