package org.dromara.common.excel;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.common.excel.annotation.CellMerge;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.common.excel.core.CellMergeHandler;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ExcelBuilder 单元测试")
class ExcelBuilderTest {

    /**
     * 验证内存导出同时应用表头样式、批注、字典和枚举下拉、外部下拉及单元格合并。
     */
    @Test
    @DisplayName("构建带内部增强的 Excel 工作簿")
    void shouldBuildWorkbookWithInternalEnhancements() throws Exception {
        byte[] bytes = buildWorkbook();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("report", workbook.getSheetAt(0).getSheetName());
            assertFalse(workbook.getSheetAt(0).getDataValidations().isEmpty());
            assertFalse(workbook.getSheetAt(0).getMergedRegions().isEmpty());
            assertEquals("填写业务分类", workbook.getSheetAt(0).getRow(0).getCell(0).getCellComment().getString().getString());
            assertTrue(workbook.getNumberOfSheets() >= 3);
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("options_0")));
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("linkedOptions_0")));
        }
    }

    /**
     * 验证通过构造器写出的工作簿可按工作表编号、名称和读取选项重新解析为对象。
     */
    @Test
    @DisplayName("写入并读取 Excel 数据")
    void shouldRoundTripWorkbookThroughReadBuilder() {
        byte[] bytes = buildWorkbook();

        List<ExportRow> rows = ExcelBuilder.read(new ByteArrayInputStream(bytes), ExportRow.class)
            .validate(false)
            .failFast(false)
            .sheetNo(0)
            .sheetName("report")
            .headRowNumber(1)
            .ignoreEmptyRow(true)
            .autoTrim(true)
            .autoStrip(true)
            .numRows(20)
            .doReadSync();

        assertEquals(3, rows.size());
        assertEquals("A", rows.getFirst().getCategory());
        assertEquals("1", rows.getFirst().getStatus());
        assertEquals(1, rows.getFirst().getLevel());
    }

    /**
     * 验证构造器拒绝非法尺寸、ZIP 分页和空模板数据，避免生成不可用文件。
     */
    @Test
    @DisplayName("校验 Excel 构造参数")
    void shouldValidateBuilderArguments() {
        ExcelBuilder<ExportRow> builder = ExcelBuilder.of(rows(), ExportRow.class);

        assertThrows(IllegalArgumentException.class, () -> builder.columnWidth(0));
        assertThrows(IllegalArgumentException.class, () -> builder.rowHeight((short) 0, (short) 10));
        assertThrows(IllegalArgumentException.class, () -> builder.zip(0));
        assertThrows(UnsupportedOperationException.class,
            () -> builder.zip(2).toStream(new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
            () -> ExcelBuilder.template("missing.xlsx").data(List.of()).toStream(new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
            () -> ExcelBuilder.template("missing.xlsx").multiList(Map.of()).toStream(new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
            () -> ExcelBuilder.template("missing.xlsx").multiSheet(List.of()).toStream(new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
            () -> ExcelBuilder.read(new ByteArrayInputStream(new byte[0]), ExportRow.class).headRowNumber(-1));
        assertThrows(IllegalArgumentException.class,
            () -> ExcelBuilder.read(new ByteArrayInputStream(new byte[0]), ExportRow.class).numRows(0));
    }

    /**
     * 验证合并处理器按依赖字段切断重复段，空值中断合并，并正确计算多级表头偏移。
     */
    @Test
    @DisplayName("计算依赖字段单元格合并区域")
    void shouldCalculateConditionalMergeRanges() {
        List<MergeRow> rows = List.of(
            new MergeRow("A", "east"),
            new MergeRow("A", "east"),
            new MergeRow("A", "west"),
            new MergeRow("", "west"),
            new MergeRow("B", "west"),
            new MergeRow("B", "west"));

        List<String> ranges = CellMergeHandler.of(true).handle(rows).stream()
            .map(range -> range.formatAsString())
            .sorted()
            .toList();

        assertEquals(List.of("A3:A4", "A7:A8"), ranges);
        assertEquals("A1:A2", CellMergeHandler.of(false, 9).handle(rows.subList(0, 2)).getFirst().formatAsString());
        assertTrue(CellMergeHandler.of().handle(List.of()).isEmpty());
        assertTrue(CellMergeHandler.of().handle(List.of(new Object())).isEmpty());
    }

    /**
     * 创建覆盖简单、额外 Sheet 和级联下拉分支的内存工作簿。
     *
     * @return XLSX 字节数组
     */
    private static byte[] buildWorkbook() {
        List<String> manyOptions = IntStream.rangeClosed(1, 11).mapToObj(i -> "选项" + i).toList();
        List<DropDownOptions> options = List.of(
            new DropDownOptions(3, List.of("是", "否")),
            new DropDownOptions(4, manyOptions),
            new DropDownOptions(5, 6, List.of("父级_1"), Map.of("父级_1", List.of("子级_1", "子级_2"))));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ExcelBuilder.of(rows(), ExportRow.class)
            .sheetName("report")
            .sheetNo(0)
            .merge()
            .options(options)
            .needHead(true)
            .automaticMergeHead(true)
            .columnWidth(18)
            .rowHeight((short) 22, (short) 18)
            .toStream(outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 创建用于导出和读取往返的测试数据。
     *
     * @return 测试数据行
     */
    private static List<ExportRow> rows() {
        return List.of(
            new ExportRow("A", "1", 1),
            new ExportRow("A", "0", 2),
            new ExportRow("B", "1", 1));
    }

    @ExcelIgnoreUnannotated
    public static class ExportRow {

        @ExcelProperty("分类")
        @CellMerge
        @ExcelRequired
        @ExcelNotation("填写业务分类")
        private String category;

        @ExcelProperty("状态")
        @ExcelDictFormat(readConverterExp = "0=停用,1=启用")
        private String status;

        @ExcelProperty("级别")
        @ExcelEnumFormat(enumClass = Level.class)
        private Integer level;

        /**
         * 创建供 Excel 反射实例化的空对象。
         */
        public ExportRow() {
        }

        /**
         * 创建包含导出字段的测试数据行。
         *
         * @param category 分类
         * @param status   状态编码
         * @param level    级别编码
         */
        public ExportRow(String category, String status, Integer level) {
            this.category = category;
            this.status = status;
            this.level = level;
        }

        /**
         * 返回业务分类。
         *
         * @return 业务分类
         */
        public String getCategory() {
            return category;
        }

        /**
         * 设置业务分类供 Excel 导入使用。
         *
         * @param category 业务分类
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * 返回状态编码。
         *
         * @return 状态编码
         */
        public String getStatus() {
            return status;
        }

        /**
         * 设置状态编码供 Excel 导入使用。
         *
         * @param status 状态编码
         */
        public void setStatus(String status) {
            this.status = status;
        }

        /**
         * 返回级别编码。
         *
         * @return 级别编码
         */
        public Integer getLevel() {
            return level;
        }

        /**
         * 设置级别编码供 Excel 导入使用。
         *
         * @param level 级别编码
         */
        public void setLevel(Integer level) {
            this.level = level;
        }
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
         * 返回级别编码供下拉处理器读取。
         *
         * @return 级别编码
         */
        public int getCode() {
            return code;
        }

        /**
         * 返回级别文本供下拉处理器读取。
         *
         * @return 级别文本
         */
        public String getText() {
            return text;
        }
    }

    @ExcelIgnoreUnannotated
    private static class MergeRow {

        @ExcelProperty({"业务", "分类"})
        @CellMerge(mergeBy = "region")
        private final String category;

        @ExcelProperty({"业务", "区域"})
        private final String region;

        /**
         * 创建用于验证依赖字段合并规则的数据行。
         *
         * @param category 分类
         * @param region   区域
         */
        private MergeRow(String category, String region) {
            this.category = category;
            this.region = region;
        }

        /**
         * 返回待合并分类。
         *
         * @return 分类
         */
        public String getCategory() {
            return category;
        }

        /**
         * 返回控制分类是否允许合并的区域。
         *
         * @return 区域
         */
        public String getRegion() {
            return region;
        }
    }
}
