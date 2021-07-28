package com.ruoyi.generator.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;
import com.ruoyi.generator.mapper.GenTableColumnMapper;
import com.ruoyi.generator.mapper.GenTableMapper;
import com.ruoyi.generator.util.GenUtils;
import com.ruoyi.generator.util.VelocityInitializer;
import com.ruoyi.generator.util.VelocityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 业务 服务层实现
 *
 * @author ruoyi
 */
@Slf4j
@Service
public class GenTableServiceImpl extends ServicePlusImpl<GenTableMapper, GenTable, GenTable> implements IGenTableService {

    @Autowired
    private GenTableColumnMapper genTableColumnMapper;

    /**
     * 查询业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    @Override
    public GenTable selectGenTableById(Long id) {
        GenTable genTable = baseMapper.selectGenTableById(id);
        setTableFromOptions(genTable);
        return genTable;
    }

    @Override
    public TableDataInfo<GenTable> selectPageGenTableList(GenTable genTable) {
        return PageUtils.buildDataInfo(baseMapper.selectPageGenTableList(PageUtils.buildPage(), genTable));
    }

    @Override
    public TableDataInfo<GenTable> selectPageDbTableList(GenTable genTable) {
        return PageUtils.buildDataInfo(baseMapper.selectPageDbTableList(PageUtils.buildPage(), genTable));
    }

    /**
     * 查询业务列表
     *
     * @param genTable 业务信息
     * @return 业务集合
     */
    @Override
    public List<GenTable> selectGenTableList(GenTable genTable) {
        return baseMapper.selectGenTableList(genTable);
    }

    /**
     * 查询据库列表
     *
     * @param genTable 业务信息
     * @return 数据库表集合
     */
    @Override
    public List<GenTable> selectDbTableList(GenTable genTable) {
        return baseMapper.selectDbTableList(genTable);
    }

    /**
     * 查询据库列表
     *
     * @param tableNames 表名称组
     * @return 数据库表集合
     */
    @Override
    public List<GenTable> selectDbTableListByNames(String[] tableNames) {
        return baseMapper.selectDbTableListByNames(tableNames);
    }

    /**
     * 查询所有表信息
     *
     * @return 表信息集合
     */
    @Override
    public List<GenTable> selectGenTableAll() {
        return baseMapper.selectGenTableAll();
    }

    /**
     * 修改业务
     *
     * @param genTable 业务信息
     * @return 结果
     */
    @Override
    @Transactional
    public void updateGenTable(GenTable genTable) {
        String options = JsonUtils.toJsonString(genTable.getParams());
        genTable.setOptions(options);
        int row = baseMapper.updateById(genTable);
        if (row > 0) {
            for (GenTableColumn cenTableColumn : genTable.getColumns()) {
                genTableColumnMapper.update(cenTableColumn,
                        new LambdaUpdateWrapper<GenTableColumn>()
							.set(StrUtil.isBlank(cenTableColumn.getColumnComment()), GenTableColumn::getColumnComment, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsPk()), GenTableColumn::getIsPk, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsIncrement()), GenTableColumn::getIsIncrement, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsInsert()), GenTableColumn::getIsInsert, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsEdit()), GenTableColumn::getIsEdit, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsList()), GenTableColumn::getIsList, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsQuery()), GenTableColumn::getIsQuery, null)
							.set(StrUtil.isBlank(cenTableColumn.getIsRequired()), GenTableColumn::getIsRequired, null)
							.set(StrUtil.isBlank(cenTableColumn.getDictType()), GenTableColumn::getDictType, "")
							.eq(GenTableColumn::getColumnId,cenTableColumn.getColumnId()));
            }
        }
    }

    /**
     * 删除业务对象
     *
     * @param tableIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public void deleteGenTableByIds(Long[] tableIds) {
        List<Long> ids = Arrays.asList(tableIds);
        removeByIds(ids);
        genTableColumnMapper.delete(new LambdaQueryWrapper<GenTableColumn>().in(GenTableColumn::getTableId, ids));
    }

    /**
     * 导入表结构
     *
     * @param tableList 导入表列表
     */
    @Override
    @Transactional
    public void importGenTable(List<GenTable> tableList) {
        String operName = SecurityUtils.getUsername();
        try {
            for (GenTable table : tableList) {
                String tableName = table.getTableName();
                GenUtils.initTable(table, operName);
                int row = baseMapper.insert(table);
                if (row > 0) {
                    // 保存列信息
                    List<GenTableColumn> genTableColumns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
                    for (GenTableColumn column : genTableColumns) {
                        GenUtils.initColumnField(column, table);
						genTableColumnMapper.insert(column);
                    }
				}
            }
        } catch (Exception e) {
            throw new CustomException("导入失败：" + e.getMessage());
        }
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
        // 查询表信息
        GenTable table = baseMapper.selectGenTableById(tableId);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);
        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            dataMap.put(template, sw.toString());
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）
     *
     * @param tableName 表名称
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String tableName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        generatorCode(tableName, zip);
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * 生成代码（自定义路径）
     *
     * @param tableName 表名称
     */
    @Override
    public void generatorCode(String tableName) {
        // 查询表信息
        GenTable table = baseMapper.selectGenTableByName(tableName);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
        for (String template : templates) {
            if (!StrUtil.containsAny("sql.vm", "api.js.vm", "index.vue.vm", "index-tree.vue.vm", template)) {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                tpl.merge(context, sw);
                String path = getGenPath(table, template);
                FileUtils.writeUtf8String(sw.toString(), path);
            }
        }
    }

    /**
     * 同步数据库
     *
     * @param tableName 表名称
     */
    @Override
    @Transactional
    public void synchDb(String tableName) {
        GenTable table = baseMapper.selectGenTableByName(tableName);
        List<GenTableColumn> tableColumns = table.getColumns();
        List<String> tableColumnNames = tableColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

        List<GenTableColumn> dbTableColumns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
        if (Validator.isEmpty(dbTableColumns)) {
            throw new CustomException("同步数据失败，原表结构不存在");
        }
        List<String> dbTableColumnNames = dbTableColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

        dbTableColumns.forEach(column -> {
            if (!tableColumnNames.contains(column.getColumnName())) {
                GenUtils.initColumnField(column, table);
				genTableColumnMapper.insert(column);
			}
		});

        List<GenTableColumn> delColumns = tableColumns.stream().filter(column -> !dbTableColumnNames.contains(column.getColumnName())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(delColumns)) {
            List<Long> ids = delColumns.stream().map(GenTableColumn::getColumnId).collect(Collectors.toList());
            genTableColumnMapper.deleteBatchIds(ids);
        }
    }

    /**
     * 批量生成代码（下载方式）
     *
     * @param tableNames 表数组
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableName : tableNames) {
            generatorCode(tableName, zip);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * 查询表信息并生成代码
     */
    private void generatorCode(String tableName, ZipOutputStream zip) {
        // 查询表信息
        GenTable table = baseMapper.selectGenTableByName(tableName);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            try {
                // 添加到zip
                zip.putNextEntry(new ZipEntry(VelocityUtils.getFileName(template, table)));
                IOUtils.write(sw.toString(), zip, Constants.UTF8);
                IOUtils.closeQuietly(sw);
                zip.flush();
                zip.closeEntry();
            } catch (IOException e) {
                log.error("渲染模板失败，表名：" + table.getTableName(), e);
            }
        }
    }

    /**
     * 修改保存参数校验
     *
     * @param genTable 业务信息
     */
    @Override
    public void validateEdit(GenTable genTable) {
        if (GenConstants.TPL_TREE.equals(genTable.getTplCategory())) {
			Map<String, Object> paramsObj = genTable.getParams();
            if (Validator.isEmpty(paramsObj.get(GenConstants.TREE_CODE))) {
                throw new CustomException("树编码字段不能为空");
            } else if (Validator.isEmpty(paramsObj.get(GenConstants.TREE_PARENT_CODE))) {
                throw new CustomException("树父编码字段不能为空");
            } else if (Validator.isEmpty(paramsObj.get(GenConstants.TREE_NAME))) {
                throw new CustomException("树名称字段不能为空");
            } else if (GenConstants.TPL_SUB.equals(genTable.getTplCategory())) {
                if (Validator.isEmpty(genTable.getSubTableName())) {
                    throw new CustomException("关联子表的表名不能为空");
                } else if (Validator.isEmpty(genTable.getSubTableFkName())) {
                    throw new CustomException("子表关联的外键名不能为空");
                }
            }
        }
    }

    /**
     * 设置主键列信息
     *
     * @param table 业务表信息
     */
    public void setPkColumn(GenTable table) {
        for (GenTableColumn column : table.getColumns()) {
            if (column.isPk()) {
                table.setPkColumn(column);
                break;
            }
        }
        if (Validator.isNull(table.getPkColumn())) {
            table.setPkColumn(table.getColumns().get(0));
        }
        if (GenConstants.TPL_SUB.equals(table.getTplCategory())) {
            for (GenTableColumn column : table.getSubTable().getColumns()) {
                if (column.isPk()) {
                    table.getSubTable().setPkColumn(column);
                    break;
                }
            }
            if (Validator.isNull(table.getSubTable().getPkColumn())) {
                table.getSubTable().setPkColumn(table.getSubTable().getColumns().get(0));
            }
        }
    }

    /**
     * 设置主子表信息
     *
     * @param table 业务表信息
     */
    public void setSubTable(GenTable table) {
        String subTableName = table.getSubTableName();
        if (Validator.isNotEmpty(subTableName)) {
            table.setSubTable(baseMapper.selectGenTableByName(subTableName));
        }
    }

    /**
     * 设置代码生成其他选项值
     *
     * @param genTable 设置后的生成对象
     */
    public void setTableFromOptions(GenTable genTable) {
		Map<String, Object> paramsObj = JsonUtils.parseMap(genTable.getOptions());
        if (Validator.isNotNull(paramsObj)) {
            String treeCode = Convert.toStr(paramsObj.get(GenConstants.TREE_CODE));
            String treeParentCode = Convert.toStr(paramsObj.get(GenConstants.TREE_PARENT_CODE));
            String treeName = Convert.toStr(paramsObj.get(GenConstants.TREE_NAME));
            String parentMenuId = Convert.toStr(paramsObj.get(GenConstants.PARENT_MENU_ID));
            String parentMenuName = Convert.toStr(paramsObj.get(GenConstants.PARENT_MENU_NAME));

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
     * @param table 业务表信息
     * @param template 模板文件路径
     * @return 生成地址
     */
    public static String getGenPath(GenTable table, String template) {
        String genPath = table.getGenPath();
        if (StrUtil.equals(genPath, "/")) {
            return System.getProperty("user.dir") + File.separator + "src" + File.separator + VelocityUtils.getFileName(template, table);
        }
        return genPath + File.separator + VelocityUtils.getFileName(template, table);
    }
}
