package org.dromara.gen.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.gen.domain.GenTable;
import org.dromara.gen.domain.GenTableColumn;
import org.dromara.gen.service.IGenTableService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成 操作处理
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/tool/gen")
public class GenController extends BaseController {

    private final IGenTableService genTableService;

    /**
     * 分页查询代码生成业务列表。
     *
     * @param genTable 查询条件
     * @param pageQuery 分页参数
     * @return 代码生成列表
     */
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/list")
    public R<PageResult<GenTable>> genList(GenTable genTable, PageQuery pageQuery) {
        return R.ok(genTableService.selectPageGenTableList(genTable, pageQuery));
    }

    /**
     * 修改代码生成业务
     *
     * @param tableId 表ID
     * @return 表、字段与可选业务表信息
     */
    @RepeatSubmit()
    @SaCheckPermission("tool:gen:query")
    @GetMapping(value = "/{tableId}")
    public R<Map<String, Object>> getInfo(@PathVariable Long tableId) {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> tables = genTableService.selectGenTableAll();
        List<GenTableColumn> list = genTableService.selectGenTableColumnListByTableId(tableId);
        Map<String, Object> map = new HashMap<>(3);
        map.put("info", table);
        map.put("rows", list);
        map.put("tables", tables);
        return R.ok(map);
    }

    /**
     * 分页查询数据库表列表。
     *
     * @param genTable 查询条件
     * @param pageQuery 分页参数
     * @return 数据库表列表
     */
    @SaCheckPermission("tool:gen:list")
    @GetMapping("/db/list")
    public R<PageResult<GenTable>> dataList(GenTable genTable, PageQuery pageQuery) {
        return R.ok(genTableService.selectPageDbTableList(genTable, pageQuery));
    }

    /**
     * 查询数据表字段列表
     *
     * @param tableId 表ID
     * @return 字段列表
     */
    @SaCheckPermission("tool:gen:list")
    @GetMapping(value = "/column/{tableId}")
    public R<PageResult<GenTableColumn>> columnList(@PathVariable("tableId") Long tableId) {
        List<GenTableColumn> list = genTableService.selectGenTableColumnListByTableId(tableId);
        return R.ok(PageResult.build(list));
    }

    /**
     * 导入表结构（保存）
     *
     * @param tables   表名串
     * @param dataName 数据源名称
     * @return 操作结果
     */
    @SaCheckPermission("tool:gen:import")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @Lock4j(keys = {"#dataName"}, acquireTimeout = 10000)
    @RepeatSubmit()
    @PostMapping("/importTable")
    public R<Void> importTableSave(String tables, String dataName) {
        String[] tableNames = Convert.toStrArray(tables);
        // 查询表信息
        List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames, dataName);
        genTableService.importGenTable(tableList, dataName);
        return R.ok();
    }

    /**
     * 保存代码生成业务配置。
     *
     * @param genTable 业务配置
     * @return 操作结果
     */
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> editSave(@Validated @RequestBody GenTable genTable) {
        genTableService.validateEdit(genTable);
        genTableService.updateGenTable(genTable);
        return R.ok();
    }

    /**
     * 删除代码生成
     *
     * @param tableIds 表ID串
     * @return 操作结果
     */
    @SaCheckPermission("tool:gen:remove")
    @Log(title = "代码生成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tableIds}")
    public R<Void> remove(@PathVariable Long[] tableIds) {
        genTableService.deleteGenTableByIds(tableIds);
        return R.ok();
    }

    /**
     * 预览代码
     *
     * @param tableId 表ID
     * @return 模板路径与生成代码内容映射
     */
    @SaCheckPermission("tool:gen:preview")
    @GetMapping("/preview/{tableId}")
    public R<Map<String, String>> preview(@PathVariable("tableId") Long tableId) throws IOException {
        Map<String, String> dataMap = genTableService.previewCode(tableId);
        return R.ok(dataMap);
    }

    /**
     * 生成代码（下载方式）
     *
     * @param response HTTP 响应
     * @param tableId 表ID
     */
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/download/{tableId}")
    public void download(HttpServletResponse response, @PathVariable("tableId") Long tableId) throws IOException {
        byte[] data = genTableService.downloadCode(tableId);
        genCode(response, data);
    }

    /**
     * 生成代码（自定义路径）
     *
     * @param tableId 表ID
     * @return 操作结果
     */
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/genCode/{tableId}")
    public R<Void> genCode(@PathVariable("tableId") Long tableId) {
        genTableService.generatorCode(tableId);
        return R.ok();
    }

    /**
     * 同步数据库
     *
     * @param tableId 表ID
     * @return 操作结果
     */
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @Lock4j(keys = {"#tableId"}, acquireTimeout = 5000)
    @GetMapping("/synchDb/{tableId}")
    public R<Void> synchDb(@PathVariable("tableId") Long tableId) {
        genTableService.synchDb(tableId);
        return R.ok();
    }

    /**
     * 批量生成代码
     *
     * @param response HTTP 响应
     * @param tableIdStr 表ID串
     */
    @SaCheckPermission("tool:gen:code")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/batchGenCode")
    public void batchGenCode(HttpServletResponse response, String tableIdStr) throws IOException {
        String[] tableIds = Convert.toStrArray(tableIdStr);
        byte[] data = genTableService.downloadCode(tableIds);
        genCode(response, data);
    }

    /**
     * 将生成结果写出为 zip 文件流。
     *
     * @param response HTTP 响应
     * @param data zip 二进制数据
     */
    private void genCode(HttpServletResponse response, byte[] data) throws IOException {
        response.reset();
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment; filename=\"ruoyi.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IoUtil.write(response.getOutputStream(), false, data);
    }

    /**
     * 查询当前可用数据源名称列表。
     *
     * @return 数据源名称集合
     */
    @SaCheckPermission("tool:gen:list")
    @GetMapping(value = "/getDataNames")
    public R<Object> getCurrentDataSourceNameList() {
        return R.ok(DataBaseHelper.getDataSourceNameList());
    }
}
