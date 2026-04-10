package org.dromara.gen.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anyline.metadata.Column;
import org.anyline.metadata.Table;
import org.anyline.proxy.ServiceProxy;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.gen.constant.GenConstants;
import org.dromara.gen.domain.GenTable;
import org.dromara.gen.domain.GenTableColumn;
import org.dromara.gen.mapper.GenTableColumnMapper;
import org.dromara.gen.mapper.GenTableMapper;
import org.dromara.gen.util.GenUtils;
import org.dromara.gen.util.TemplateEngineUtils;
import org.dromara.gen.util.template.PathNamedTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 业务 服务层实现
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GenTableServiceImpl implements IGenTableService {

    private final GenTableMapper baseMapper;
    private final GenTableColumnMapper genTableColumnMapper;

    private static final String[] TABLE_IGNORE = new String[]{"sj_", "flow_", "gen_"};

    /**
     * 查询业务字段列表
     *
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    @Override
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId) {
        return genTableColumnMapper.selectList(new LambdaQueryWrapper<GenTableColumn>()
            .eq(GenTableColumn::getTableId, tableId)
            .orderByAsc(GenTableColumn::getSort));
    }

    /**
     * 查询业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    @Override
    public GenTable selectGenTableById(Long id) {
        GenTable genTable = getGenTable(id);
        setTableFromOptions(genTable);
        return genTable;
    }

    /**
     * 分页查询已导入的代码生成业务表。
     *
     * @param genTable  业务表筛选条件
     * @param pageQuery 分页参数
     * @return 业务表分页结果
     */
    @Override
    public PageResult<GenTable> selectPageGenTableList(GenTable genTable, PageQuery pageQuery) {
        Page<GenTable> page = baseMapper.selectPage(pageQuery.build(), this.buildGenTableQueryWrapper(genTable));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 构造代码生成业务表查询条件。
     *
     * @param genTable 业务表筛选条件
     * @return 包含数据源、表名、表注释和时间区间的查询包装器
     */
    private QueryWrapper<GenTable> buildGenTableQueryWrapper(GenTable genTable) {
        Map<String, Object> params = genTable.getParams();
        QueryWrapper<GenTable> wrapper = Wrappers.query();
        wrapper
            .eq(StringUtils.isNotEmpty(genTable.getDataName()), "data_name", genTable.getDataName())
            .like(StringUtils.isNotBlank(genTable.getTableName()), "lower(table_name)", StringUtils.lowerCase(genTable.getTableName()))
            .like(StringUtils.isNotBlank(genTable.getTableComment()), "lower(table_comment)", StringUtils.lowerCase(genTable.getTableComment()))
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "create_time", params.get("beginTime"), params.get("endTime"))
            .orderByDesc("update_time");
        return wrapper;
    }

    /**
     * 查询数据库列表
     *
     * @param genTable  包含查询条件的GenTable对象
     * @param pageQuery 包含分页信息的PageQuery对象
     * @return 包含分页结果的TableDataInfo对象
     */
    @DS("#genTable.dataName")
    @Override
    public PageResult<GenTable> selectPageDbTableList(GenTable genTable, PageQuery pageQuery) {
        // 获取查询条件
        String tableName = genTable.getTableName();
        String tableComment = genTable.getTableComment();

        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.metadata().tables();
        if (CollUtil.isEmpty(tablesMap)) {
            return PageResult.build();
        }
        List<String> tableNames = baseMapper.selectTableNameList(genTable.getDataName());
        String[] tableArrays;
        if (CollUtil.isNotEmpty(tableNames)) {
            tableArrays = tableNames.toArray(new String[0]);
        } else {
            tableArrays = new String[0];
        }
        // 过滤并转换表格数据
        List<GenTable> tables = tablesMap.values().stream()
            .filter(x -> !StringUtils.startWithAnyIgnoreCase(x.getName(), TABLE_IGNORE))
            .filter(x -> {
                if (CollUtil.isEmpty(tableNames)) {
                    return true;
                }
                return !StringUtils.equalsAnyIgnoreCase(x.getName(), tableArrays);
            })
            .filter(x -> {
                boolean nameMatches = true;
                boolean commentMatches = true;
                // 进行表名称的模糊查询
                if (StringUtils.isNotBlank(tableName)) {
                    nameMatches = StringUtils.containsIgnoreCase(x.getName(), tableName);
                }
                // 进行表描述的模糊查询
                if (StringUtils.isNotBlank(tableComment)) {
                    commentMatches = StringUtils.containsIgnoreCase(x.getComment(), tableComment);
                }
                // 同时匹配名称和描述
                return nameMatches && commentMatches;
            })
            .map(x -> {
                GenTable gen = new GenTable();
                gen.setTableName(x.getName());
                gen.setTableComment(x.getComment());
                // postgresql的表元数据没有创建时间这个东西(好奇葩) 只能new Date代替
                Date createDate = ObjectUtil.defaultIfNull(x.getCreateTime(), new Date());
                gen.setCreateTime(LocalDateTimeUtil.of(createDate));
                gen.setUpdateTime(x.getUpdateTime() != null ? LocalDateTimeUtil.of(x.getUpdateTime()) : null);
                return gen;
            }).sorted(Comparator.comparing(GenTable::getCreateTime).reversed())
            .toList();
        // 根据原始数据列表和分页参数，构建表格分页数据对象（用于假分页）
        if (CollUtil.isEmpty(tables)) {
            return PageResult.build();
        }
        Page<Object> page = pageQuery.build();
        List<GenTable> pageList = CollUtil.page((int) page.getCurrent() - 1, (int) page.getSize(), tables);
        return PageResult.build(pageList, tables.size());
    }

    /**
     * 查询据库列表
     *
     * @param tableNames 表名称组
     * @param dataName   数据源名称
     * @return 数据库表集合
     */
    @DS("#dataName")
    @Override
    public List<GenTable> selectDbTableListByNames(String[] tableNames, String dataName) {
        Set<String> tableNameSet = new HashSet<>(List.of(tableNames));
        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.metadata().tables();

        if (CollUtil.isEmpty(tablesMap)) {
            return new ArrayList<>();
        }

        List<Table<?>> tableList = tablesMap.values().stream()
            .filter(x -> !StringUtils.startWithAnyIgnoreCase(x.getName(), TABLE_IGNORE))
            .filter(x -> tableNameSet.contains(x.getName())).toList();

        if (CollUtil.isEmpty(tableList)) {
            return new ArrayList<>();
        }
        return tableList.stream().map(x -> {
            GenTable gen = new GenTable();
            gen.setDataName(dataName);
            gen.setTableName(x.getName());
            gen.setTableComment(x.getComment());
            gen.setCreateTime(LocalDateTimeUtil.of(x.getCreateTime()));
            gen.setUpdateTime(LocalDateTimeUtil.of(x.getUpdateTime()));
            return gen;
        }).toList();
    }

    /**
     * 查询所有表信息
     *
     * @return 表信息集合
     */
    @Override
    public List<GenTable> selectGenTableAll() {
        return fillTableColumns(baseMapper.selectList(new LambdaQueryWrapper<GenTable>()
            .orderByAsc(GenTable::getTableId)));
    }

    /**
     * 修改业务
     *
     * @param genTable 业务信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateGenTable(GenTable genTable) {
        String options = JsonUtils.toJsonString(genTable.getParams());
        genTable.setOptions(options);
        int row = baseMapper.updateById(genTable);
        if (row > 0) {
            genTableColumnMapper.updateBatchById(genTable.getColumns());
        }
    }

    /**
     * 删除业务对象
     *
     * @param tableIds 需要删除的数据ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteGenTableByIds(Long[] tableIds) {
        List<Long> ids = Arrays.asList(tableIds);
        baseMapper.deleteByIds(ids);
        genTableColumnMapper.delete(new LambdaQueryWrapper<GenTableColumn>().in(GenTableColumn::getTableId, ids));
    }

    /**
     * 导入表结构
     *
     * @param tableList 导入表列表
     * @param dataName  数据源名称
     */
    @DSTransactional
    @Override
    public void importGenTable(List<GenTable> tableList, String dataName) {
        try {
            for (GenTable table : tableList) {
                String tableName = table.getTableName();
                GenUtils.initTable(table);
                table.setDataName(dataName);
                int row = baseMapper.insert(table);
                if (row > 0) {
                    // 保存列信息
                    List<GenTableColumn> genTableColumns = SpringUtils.getAopProxy(this).selectDbTableColumnsByName(tableName, dataName);
                    List<GenTableColumn> saveColumns = new ArrayList<>();
                    for (GenTableColumn column : genTableColumns) {
                        GenUtils.initColumnField(column, table);
                        saveColumns.add(column);
                    }
                    if (CollUtil.isNotEmpty(saveColumns)) {
                        genTableColumnMapper.insertBatch(saveColumns);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new ServiceException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 根据表名称查询列信息
     *
     * @param tableName 表名称
     * @param dataName  数据源名称
     * @return 列信息
     */
    @DS("#dataName")
    @Override
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName, String dataName) {
        Table<?> table = ServiceProxy.metadata().table(tableName);
        if (ObjectUtil.isNull(table)) {
            return new ArrayList<>();
        }
        LinkedHashMap<String, Column> columns = table.getColumns();
        List<GenTableColumn> tableColumns = new ArrayList<>();
        columns.forEach((columnName, column) -> {
            GenTableColumn tableColumn = new GenTableColumn();
            tableColumn.setIsPk(column.isPrimaryKey() ? "1" : "0");
            tableColumn.setColumnName(column.getName());
            tableColumn.setColumnComment(column.getComment());
            tableColumn.setColumnType(column.getOriginType().toLowerCase());
            tableColumn.setSort(column.getPosition());
            tableColumn.setIsRequired(column.isNullable() ? "0" : "1");
            tableColumn.setIsIncrement(column.isAutoIncrement() ? "1" : "0");
            tableColumns.add(tableColumn);
        });
        return tableColumns;
    }

    /**
     * 预览代码
     *
     * @param tableId 表编号
     * @return 预览数据列表
     */
    @Override
    public Map<String, String> previewCode(Long tableId) {
        Map<String, String> dataMap = new LinkedHashMap<>();
        RenderContext rc = buildRenderContext(tableId);
        for (PathNamedTemplate template : rc.templates()) {
            dataMap.put(template.getPathName(), template.render(rc.context()));
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）
     *
     * @param tableId 表名称
     * @return 数据
     */
    @Override
    public byte[] downloadCode(Long tableId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        generatorCode(tableId, zip);
        IoUtil.close(zip);
        return outputStream.toByteArray();
    }

    /**
     * 生成代码（自定义路径）
     *
     * @param tableId 表名称
     */
    @Override
    public void generatorCode(Long tableId) {
        // 查询表信息
        GenTable table = getGenTable(tableId);
        // 设置主键列信息
        setPkColumn(table);

        Dict context = TemplateEngineUtils.buildContext(table);
        // 获取模板列表
        List<PathNamedTemplate> templates = TemplateEngineUtils.getTemplateList(table.getTplCategory(), table.getDataName());
        for (PathNamedTemplate template : templates) {
            String pathName = template.getPathName();
            // 渲染模板
            if (!StringUtils.containsAny(pathName, "sql.vm", "api.ts.vm", "types.ts.vm", "index.vue.vm", "index-tree.vue.vm")) {
                // 渲染模板
                try {
                    String render = template.render(context);
                    String path = getGenPath(table, pathName);
                    FileUtils.writeUtf8String(render, path);
                } catch (Exception e) {
                    throw new ServiceException("渲染模板失败，表名：" + table.getTableName());
                }
            }
        }
    }

    /**
     * 同步数据库
     *
     * @param tableId 表名称
     */
    @DSTransactional
    @Override
    public void synchDb(Long tableId) {
        GenTable table = getGenTable(tableId);
        List<GenTableColumn> tableColumns = table.getColumns();
        Map<String, GenTableColumn> tableColumnMap = StreamUtils.toIdentityMap(tableColumns, GenTableColumn::getColumnName);

        List<GenTableColumn> dbTableColumns = SpringUtils.getAopProxy(this).selectDbTableColumnsByName(table.getTableName(), table.getDataName());
        if (CollUtil.isEmpty(dbTableColumns)) {
            throw new ServiceException("同步数据失败，原表结构不存在");
        }
        List<String> dbTableColumnNames = StreamUtils.toList(dbTableColumns, GenTableColumn::getColumnName);

        List<GenTableColumn> saveColumns = new ArrayList<>();
        dbTableColumns.forEach(column -> {
            GenUtils.initColumnField(column, table);
            if (tableColumnMap.containsKey(column.getColumnName())) {
                GenTableColumn prevColumn = tableColumnMap.get(column.getColumnName());
                column.setColumnId(prevColumn.getColumnId());
                if (column.isList()) {
                    // 如果是列表，继续保留查询方式/字典类型选项
                    column.setDictType(prevColumn.getDictType());
                    column.setQueryType(prevColumn.getQueryType());
                }
                if (StringUtils.isNotEmpty(prevColumn.getIsRequired()) && !column.isPk()
                    && (column.isInsert() || column.isEdit())
                    && ((column.isUsableColumn()) || (!column.isSuperColumn()))) {
                    // 如果是(新增/修改&非主键/非忽略及父属性)，继续保留必填/显示类型选项
                    column.setIsRequired(prevColumn.getIsRequired());
                    column.setHtmlType(prevColumn.getHtmlType());
                }
            }
            saveColumns.add(column);
        });
        if (CollUtil.isNotEmpty(saveColumns)) {
            genTableColumnMapper.insertOrUpdateBatch(saveColumns);
        }
        List<GenTableColumn> delColumns = StreamUtils.filter(tableColumns, column -> !dbTableColumnNames.contains(column.getColumnName()));
        if (CollUtil.isNotEmpty(delColumns)) {
            List<Long> ids = StreamUtils.toList(delColumns, GenTableColumn::getColumnId);
            if (CollUtil.isNotEmpty(ids)) {
                genTableColumnMapper.deleteByIds(ids);
            }
        }
    }

    /**
     * 批量生成代码（下载方式）
     *
     * @param tableIds 表ID数组
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String[] tableIds) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableId : tableIds) {
            generatorCode(Long.parseLong(tableId), zip);
        }
        IoUtil.close(zip);
        return outputStream.toByteArray();
    }

    /**
     * 查询表信息并生成代码
     *
     * @param tableId 业务表主键
     * @param zip     代码压缩输出流
     */
    private void generatorCode(Long tableId, ZipOutputStream zip) {
        RenderContext rc = buildRenderContext(tableId);
        GenTable table = rc.table();
        for (PathNamedTemplate template : rc.templates()) {
            String pathName = template.getPathName();
            try {
                String render = template.render(rc.context());
                zip.putNextEntry(new ZipEntry(TemplateEngineUtils.getFileName(pathName, table)));
                IoUtil.write(zip, StandardCharsets.UTF_8, false, render);
                zip.flush();
                zip.closeEntry();
            } catch (IOException e) {
                log.error("渲染模板失败，表名：{}", table.getTableName(), e);
            }
        }
    }

    /**
     * 构建代码渲染上下文（含表信息、菜单ID、主键列、模板列表）
     *
     * @param tableId 业务表主键
     * @return 渲染上下文
     */
    private RenderContext buildRenderContext(Long tableId) {
        GenTable table = getGenTable(tableId);
        List<Long> menuIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            menuIds.add(IdGeneratorUtil.nextLongId());
        }
        table.setMenuIds(menuIds);
        setPkColumn(table);
        Dict context = TemplateEngineUtils.buildContext(table);
        List<PathNamedTemplate> templates = TemplateEngineUtils.getTemplateList(table.getTplCategory(), table.getDataName());
        return new RenderContext(table, context, templates);
    }

    private record RenderContext(GenTable table, Dict context, List<PathNamedTemplate> templates) {
    }

    /**
     * 修改保存参数校验
     *
     * @param genTable 业务信息
     */
    @Override
    public void validateEdit(GenTable genTable) {
        if (GenConstants.TPL_TREE.equals(genTable.getTplCategory())) {
            String options = JsonUtils.toJsonString(genTable.getParams());
            Dict paramsObj = JsonUtils.parseMap(options);
            if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_CODE))) {
                throw new ServiceException("树编码字段不能为空");
            } else if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_PARENT_CODE))) {
                throw new ServiceException("树父编码字段不能为空");
            } else if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_NAME))) {
                throw new ServiceException("树名称字段不能为空");
            }
        }
    }

    /**
     * 查询业务表并补齐其列信息。
     *
     * @param tableId 业务表主键
     * @return 包含字段集合的业务表实体
     */
    private GenTable getGenTable(Long tableId) {
        GenTable table = baseMapper.selectById(tableId);
        if (ObjectUtil.isNull(table)) {
            throw new ServiceException("业务表不存在");
        }
        fillTableColumns(Collections.singletonList(table));
        return table;
    }

    /**
     * 批量填充业务表对应的字段列表。
     *
     * @param tables 业务表集合
     * @return 已填充字段信息的业务表集合
     */
    private List<GenTable> fillTableColumns(List<GenTable> tables) {
        if (CollUtil.isEmpty(tables)) {
            return tables;
        }
        List<Long> tableIds = StreamUtils.toList(tables, GenTable::getTableId);
        List<GenTableColumn> columns = genTableColumnMapper.selectList(new LambdaQueryWrapper<GenTableColumn>()
            .in(GenTableColumn::getTableId, tableIds)
            .orderByAsc(GenTableColumn::getTableId)
            .orderByAsc(GenTableColumn::getSort));
        Map<Long, List<GenTableColumn>> columnMap = StreamUtils.groupByKey(columns, GenTableColumn::getTableId);
        tables.forEach(table -> table.setColumns(columnMap.getOrDefault(table.getTableId(), new ArrayList<>())));
        return tables;
    }

    /**
     * 设置主键列信息
     *
     * @param table 业务表信息
     */
    public void setPkColumn(GenTable table) {
        if (CollUtil.isEmpty(table.getColumns())) {
            throw new ServiceException("表【" + table.getTableName() + "】字段为空，请检查表结构");
        }
        for (GenTableColumn column : table.getColumns()) {
            if (column.isPk()) {
                table.setPkColumn(column);
                break;
            }
        }
        if (ObjectUtil.isNull(table.getPkColumn())) {
            table.setPkColumn(table.getColumns().get(0));
        }

    }

    /**
     * 设置代码生成其他选项值
     *
     * @param genTable 设置后的生成对象
     */
    public void setTableFromOptions(GenTable genTable) {
        Dict paramsObj = JsonUtils.parseMap(genTable.getOptions());
        if (ObjectUtil.isNotNull(paramsObj)) {
            String treeCode = paramsObj.getStr(GenConstants.TREE_CODE);
            String treeParentCode = paramsObj.getStr(GenConstants.TREE_PARENT_CODE);
            String treeName = paramsObj.getStr(GenConstants.TREE_NAME);
            Long parentMenuId = paramsObj.getLong(GenConstants.PARENT_MENU_ID);
            String parentMenuName = paramsObj.getStr(GenConstants.PARENT_MENU_NAME);

            genTable.setTreeCode(treeCode);
            genTable.setTreeParentCode(treeParentCode);
            genTable.setTreeName(treeName);
            genTable.setParentMenuId(parentMenuId);
            genTable.setParentMenuName(parentMenuName);
        }
    }

    /**
     * 获取代码生成地址
     *
     * @param table    业务表信息
     * @param template 模板文件路径
     * @return 生成地址
     */
    public static String getGenPath(GenTable table, String template) {
        String genPath = table.getGenPath();
        if (StringUtils.equals(genPath, "/")) {
            return System.getProperty("user.dir") + File.separator + "src" + File.separator + TemplateEngineUtils.getFileName(template, table);
        }
        return genPath + File.separator + TemplateEngineUtils.getFileName(template, table);
    }
}

