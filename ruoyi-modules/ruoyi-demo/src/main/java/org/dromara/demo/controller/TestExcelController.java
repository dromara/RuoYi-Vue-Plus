package org.dromara.demo.controller;

import cn.hutool.core.collection.CollUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.demo.domain.vo.ExportDemoVo;
import org.dromara.demo.listener.ExportDemoListener;
import org.dromara.demo.service.IExportExcelService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试Excel功能
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/excel")
public class TestExcelController {

    private final IExportExcelService exportExcelService;

    /**
     * 单列表多数据
     */
    @GetMapping("/exportTemplateOne")
    public void exportTemplateOne(HttpServletResponse response) {
        Map<String, String> map = new HashMap<>();
        map.put("title", "单列表多数据");
        map.put("test1", "数据测试1");
        map.put("test2", "数据测试2");
        map.put("test3", "数据测试3");
        map.put("test4", "数据测试4");
        map.put("testTest", "666");
        List<TestObj> list = new ArrayList<>();
        list.add(new TestObj("单列表测试1", "列表测试1", "列表测试2", "列表测试3", "列表测试4"));
        list.add(new TestObj("单列表测试2", "列表测试5", "列表测试6", "列表测试7", "列表测试8"));
        list.add(new TestObj("单列表测试3", "列表测试9", "列表测试10", "列表测试11", "列表测试12"));
        ExcelBuilder.template("excel/单列表.xlsx")
            .filename("单列表.xlsx")
            .data(CollUtil.newArrayList(map, list))
            .toResponse(response);
    }

    /**
     * 多列表多数据
     */
    @GetMapping("/exportTemplateMuliti")
    public void exportTemplateMuliti(HttpServletResponse response) {
        Map<String, String> map = new HashMap<>();
        map.put("title1", "标题1");
        map.put("title2", "标题2");
        map.put("title3", "标题3");
        map.put("title4", "标题4");
        map.put("author", "Lion Li");
        List<TestObj1> list1 = new ArrayList<>();
        list1.add(new TestObj1("list1测试1", "list1测试2", "list1测试3"));
        list1.add(new TestObj1("list1测试4", "list1测试5", "list1测试6"));
        list1.add(new TestObj1("list1测试7", "list1测试8", "list1测试9"));
        List<TestObj1> list2 = new ArrayList<>();
        list2.add(new TestObj1("list2测试1", "list2测试2", "list2测试3"));
        list2.add(new TestObj1("list2测试4", "list2测试5", "list2测试6"));
        List<TestObj1> list3 = new ArrayList<>();
        list3.add(new TestObj1("list3测试1", "list3测试2", "list3测试3"));
        List<TestObj1> list4 = new ArrayList<>();
        list4.add(new TestObj1("list4测试1", "list4测试2", "list4测试3"));
        list4.add(new TestObj1("list4测试4", "list4测试5", "list4测试6"));
        list4.add(new TestObj1("list4测试7", "list4测试8", "list4测试9"));
        list4.add(new TestObj1("list4测试10", "list4测试11", "list4测试12"));
        Map<String, Object> multiListMap = new HashMap<>();
        multiListMap.put("map", map);
        multiListMap.put("data1", list1);
        multiListMap.put("data2", list2);
        multiListMap.put("data3", list3);
        multiListMap.put("data4", list4);
        ExcelBuilder.template("excel/多列表.xlsx")
            .filename("多列表.xlsx")
            .multiList(multiListMap)
            .toResponse(response);
    }

    /**
     * 导出下拉框
     *
     * @param response /
     */
    @GetMapping("/exportWithOptions")
    public void exportWithOptions(HttpServletResponse response) {
        exportExcelService.exportWithOptions(response);
    }

    /**
     * 自定义导出
     *
     * @param response /
     */
    @GetMapping("/customExport")
    public void customExport(HttpServletResponse response) {
        exportExcelService.customExport(response);
    }

    /**
     * 多个sheet导出
     */
    @GetMapping("/exportTemplateMultiSheet")
    public void exportTemplateMultiSheet(HttpServletResponse response) {
        List<TestObj1> list1 = new ArrayList<>();
        list1.add(new TestObj1("list1测试1", "list1测试2", "list1测试3"));
        list1.add(new TestObj1("list1测试4", "list1测试5", "list1测试6"));
        List<TestObj1> list2 = new ArrayList<>();
        list2.add(new TestObj1("list2测试1", "list2测试2", "list2测试3"));
        list2.add(new TestObj1("list2测试4", "list2测试5", "list2测试6"));
        List<TestObj1> list3 = new ArrayList<>();
        list3.add(new TestObj1("list3测试1", "list3测试2", "list3测试3"));
        list3.add(new TestObj1("list3测试4", "list3测试5", "list3测试6"));
        List<TestObj1> list4 = new ArrayList<>();
        list4.add(new TestObj1("list4测试1", "list4测试2", "list4测试3"));
        list4.add(new TestObj1("list4测试4", "list4测试5", "list4测试6"));

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sheetMap1 = new HashMap<>();
        sheetMap1.put("data1", list1);
        Map<String, Object> sheetMap2 = new HashMap<>();
        sheetMap2.put("data2", list2);
        Map<String, Object> sheetMap3 = new HashMap<>();
        sheetMap3.put("data3", list3);
        Map<String, Object> sheetMap4 = new HashMap<>();
        sheetMap4.put("data4", list4);

        list.add(sheetMap1);
        list.add(sheetMap2);
        list.add(sheetMap3);
        list.add(sheetMap4);
        ExcelBuilder.template("excel/多sheet列表.xlsx")
            .filename("多sheet列表")
            .multiSheet(list)
            .toResponse(response);
    }

    /**
     * 导入表格
     */
    @PostMapping(value = "/importWithOptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ExportDemoVo> importWithOptions(@RequestPart("file") MultipartFile file) throws Exception {
        // 处理解析结果
        ExcelResult<ExportDemoVo> excelResult = ExcelBuilder.read(file.getInputStream(), ExportDemoVo.class)
            .listener(new ExportDemoListener())
            .doRead();
        return excelResult.getList();
    }

    /**
     * 多列表模板填充测试对象。
     */
    @Data
    @AllArgsConstructor
    static class TestObj1 {
        /**
         * 测试字段1。
         */
        private String test1;
        /**
         * 测试字段2。
         */
        private String test2;
        /**
         * 测试字段3。
         */
        private String test3;
    }

    /**
     * 单列表模板填充测试对象。
     */
    @Data
    @AllArgsConstructor
    static class TestObj {
        /**
         * 名称。
         */
        private String name;
        /**
         * 列表字段1。
         */
        private String list1;
        /**
         * 列表字段2。
         */
        private String list2;
        /**
         * 列表字段3。
         */
        private String list3;
        /**
         * 列表字段4。
         */
        private String list4;
    }

}
