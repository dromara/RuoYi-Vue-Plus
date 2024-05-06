package org.dromara.datasource.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.datasource.domain.bo.DsFieldQueryBo;
import org.dromara.datasource.domain.bo.DsQueryBo;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.jdbc.JdbcQuery;
import org.dromara.datasource.jdbc.core.DbType;
import org.dromara.datasource.service.ISysDatasourceService;
import org.dromara.datasource.utils.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 动态数据源
 *
 * @author ixyxj
 * @date 2024-05-02
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/datasource")
public class SysDatasourceController extends BaseController {

    private final ISysDatasourceService sysDatasourceService;

    /**
     * 查询动态数据源列表
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/list")
    public TableDataInfo<SysDatasourceVo> list(SysDatasourceBo bo, PageQuery pageQuery) {
        return sysDatasourceService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出动态数据源列表
     */
    @SaCheckPermission("system:datasource:export")
    @Log(title = "动态数据源", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysDatasourceBo bo, HttpServletResponse response) {
        List<SysDatasourceVo> list = sysDatasourceService.queryList(bo);
        ExcelUtil.exportExcel(list, "动态数据源", SysDatasourceVo.class, response);
    }

    /**
     * 获取动态数据源详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:datasource:query")
    @GetMapping("/{id}")
    public R<SysDatasourceVo> getInfo(@NotNull(message = "主键不能为空")
                                      @PathVariable Long id) {
        return R.ok(sysDatasourceService.queryById(id));
    }

    /**
     * 新增动态数据源
     */
    @SaCheckPermission("system:datasource:add")
    @Log(title = "动态数据源", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysDatasourceBo bo) {
        return toAjax(sysDatasourceService.insertByBo(bo));
    }

    /**
     * 修改动态数据源
     */
    @SaCheckPermission("datasource:datasource:edit")
    @Log(title = "动态数据源", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysDatasourceBo bo) {
        return toAjax(sysDatasourceService.updateByBo(bo));
    }

    /**
     * 删除动态数据源
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:datasource:remove")
    @Log(title = "动态数据源", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysDatasourceService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 获取数据源类型
     */
    @SaCheckPermission("system:datasource:query")
    @GetMapping("/types")
    public R<DbType[]> types() {
        return R.ok(DbType.values());
    }

    /**
     * 获取数据库列表
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/databases")
    public R<List<String>> databases(@Validated @RequestBody DsQueryBo bo) {
        List<String> query = sysDatasourceService.getJdbcService().query(bo.getDsName(), bo.getDsType(), JdbcQuery::queryDatabases);
        return R.ok(query);
    }

    /**
     * 获取表列表
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/tables")
    public R<List<Map<String, Object>>> tables(@Validated @RequestBody DsQueryBo bo) {
        List<Map<String, Object>> query = sysDatasourceService.getJdbcService().query(bo.getDsName(), bo.getDsType(), jdbcQuery -> jdbcQuery.querySchemaTables(bo.getSchema(), null));
        return R.ok(query);
    }

    /**
     * 获取字段列表
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/columns")
    public R<List<Map<String, Object>>> columns(@Validated @RequestBody DsQueryBo bo) {
        List<Map<String, Object>> query = sysDatasourceService.getJdbcService().query(bo.getDsName(), bo.getDsType(), jdbcQuery -> jdbcQuery.queryTableFields(bo.getSchema(), bo.getTable()));
        return R.ok(query);
    }

    /**
     * 查询sql脚本
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/query")
    public R<Object> query(@Validated @RequestBody DsQueryBo bo) {
        String sqlScript = StrUtil.removeSuffix(bo.getSqlScript(), ";");
        Assert.isTrue(StrUtil.isNotBlank(sqlScript), "sql脚本不能为空");
        List<?> query = sysDatasourceService.getJdbcService().query(bo.getDsName(), bo.getDsType(),
            jdbcQuery -> {
                JdbcTemplate jdbc = jdbcQuery.getJdbc();
                if (StrUtil.isNotEmpty(bo.getSchema())) {
                    jdbc.execute("use `%s`".formatted(bo.getSchema()));
                }

                if (StrUtil.contains(sqlScript, ";")) {
                    return Arrays.stream(sqlScript.split(";"))
                        .distinct()
                        .filter(StrUtil::isNotBlank)
                        .map(String::trim)
                        .map(jdbc::queryForList)
                        .toList();
                } else {
                    return jdbc.queryForList(sqlScript);
                }
            });
        return R.ok(query);
    }

    /**
     * 查询sql脚本
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/execute")
    public R<List<Map<String, Object>>> execute(@Validated @RequestBody DsQueryBo bo) {
        String sqlScript = StrUtil.removeSuffix(bo.getSqlScript(), ";");
        Assert.isTrue(StrUtil.isNotBlank(sqlScript), "sql脚本不能为空");
        sysDatasourceService.getJdbcService().execute(bo.getDsName(), bo.getDsType(),
            jdbcQuery -> {
                JdbcTemplate jdbc = jdbcQuery.getJdbc();
                if (StrUtil.isNotEmpty(bo.getSchema())) {
                    jdbc.execute("use `%s`".formatted(bo.getSchema()));
                }

                Arrays.stream(sqlScript.split(";"))
                    .distinct()
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .forEach(jdbc::execute);
            });
        return R.ok("执行成功");
    }


    /**
     * 测试数据源连接
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/test")
    public R<Boolean> testConn(@Validated @RequestBody SysDatasourceBo bo) {
        return R.ok(sysDatasourceService.testConnByBo(bo));
    }

    /**
     * 查询字段数据
     */
    @SaCheckPermission("system:datasource:query")
    @PostMapping("/fieldData")
    public R<Object> queryFieldData(@Validated @RequestBody DsFieldQueryBo bo) {
        return R.ok(sysDatasourceService.queryFieldData(bo));
    }
}
