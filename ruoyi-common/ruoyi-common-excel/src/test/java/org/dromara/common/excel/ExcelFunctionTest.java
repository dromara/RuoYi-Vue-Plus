package org.dromara.common.excel;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.excel.core.DefaultExcelResult;
import org.dromara.common.excel.core.DropDownOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("common-excel 功能单元测试")
class ExcelFunctionTest {

    /**
     * 验证级联下拉选项值能够安全拼接和还原，确保生成的名称符合 Excel 名称规则。
     */
    @Test
    @DisplayName("创建并解析 Excel 下拉选项")
    void shouldCreateAndAnalyzeDropDownOptionValue() {
        String option = DropDownOptions.createOptionValue("华东", 1001);

        assertEquals("华东_1001", option);
        assertEquals(List.of("华东", "1001"), DropDownOptions.analyzeOptionValue(option));
    }

    /**
     * 验证数字开头、单元格引用和特殊字符不会进入 Excel 名称管理器。
     */
    @Test
    @DisplayName("拒绝非法 Excel 下拉选项")
    void shouldRejectInvalidDropDownOptionValue() {
        assertThrows(ServiceException.class, () -> DropDownOptions.createOptionValue(1001, "华东"));
        assertThrows(ServiceException.class, () -> DropDownOptions.validateOptionValue("A1"));
        assertThrows(ServiceException.class, () -> DropDownOptions.createOptionValue("华东-一区"));
    }

    /**
     * 验证 Excel 导入结果对全失败、全成功和部分成功场景生成准确统计文案。
     */
    @Test
    @DisplayName("汇总 Excel 导入结果")
    void shouldSummarizeExcelImportResult() {
        assertEquals("读取失败，未解析到数据", new DefaultExcelResult<>(List.of(), List.of("格式错误")).getAnalysis());
        assertEquals("恭喜您，全部读取成功！共2条",
            new DefaultExcelResult<>(List.of("a", "b"), List.of()).getAnalysis());
        assertEquals("共3条，成功导入2条，错误1条",
            new DefaultExcelResult<>(List.of("a", "b"), List.of("格式错误")).getAnalysis());
    }

    /**
     * 验证父子数据按父 ID 构建级联下拉，且没有有效父项的子数据不会进入结果。
     */
    @Test
    @DisplayName("构建父子级联下拉选项")
    void shouldBuildLinkedDropDownOptions() {
        List<OptionNode> parents = List.of(
            new OptionNode(1L, null, "华东_1"),
            new OptionNode(2L, null, "华北_2"));
        List<OptionNode> children = List.of(
            new OptionNode(11L, 1L, "上海_11"),
            new OptionNode(12L, 1L, "杭州_12"),
            new OptionNode(21L, 2L, "北京_21"),
            new OptionNode(99L, 9L, "孤立_99"));

        DropDownOptions result = DropDownOptions.buildLinkedOptions(
            parents, 0, children, 1, OptionNode::id, OptionNode::parentId, OptionNode::label);

        assertEquals(0, result.getIndex());
        assertEquals(1, result.getNextIndex());
        assertEquals(List.of("华东_1", "华北_2"), result.getOptions());
        assertEquals(Map.of(
            "华东_1", List.of("上海_11", "杭州_12"),
            "华北_2", List.of("北京_21")), result.getNextOptions());
    }

    private record OptionNode(Long id, Long parentId, String label) {
    }
}
