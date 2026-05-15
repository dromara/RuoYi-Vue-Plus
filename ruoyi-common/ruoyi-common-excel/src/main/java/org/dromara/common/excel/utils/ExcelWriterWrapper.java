package org.dromara.common.excel.utils;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.context.WriteContext;
import org.apache.fesod.sheet.write.builder.ExcelWriterSheetBuilder;
import org.apache.fesod.sheet.write.builder.ExcelWriterTableBuilder;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.WriteTable;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * ExcelWriterWrapper Excel写出包装器
 * <br>
 * 提供了一组与 ExcelWriter 一一对应的写出方法，避免直接提供 ExcelWriter 而导致的一些不可控问题（比如提前关闭了IO流等）
 *
 * @author 秋辞未寒
 * @see ExcelWriter
 */
public record ExcelWriterWrapper<T>(ExcelWriter excelWriter) {

    /**
     * 写出集合数据到指定工作表。
     *
     * @param data       数据集合
     * @param writeSheet 工作表
     */
    public void write(Collection<T> data, WriteSheet writeSheet) {
        excelWriter.write(data, writeSheet);
    }

    /**
     * 通过数据提供器写出集合数据到指定工作表。
     *
     * @param supplier   数据提供器
     * @param writeSheet 工作表
     */
    public void write(Supplier<Collection<T>> supplier, WriteSheet writeSheet) {
        excelWriter.write(supplier.get(), writeSheet);
    }

    /**
     * 写出集合数据到指定工作表和表格。
     *
     * @param data       数据集合
     * @param writeSheet 工作表
     * @param writeTable 表格
     */
    public void write(Collection<T> data, WriteSheet writeSheet, WriteTable writeTable) {
        excelWriter.write(data, writeSheet, writeTable);
    }

    /**
     * 通过数据提供器写出集合数据到指定工作表和表格。
     *
     * @param supplier   数据提供器
     * @param writeSheet 工作表
     * @param writeTable 表格
     */
    public void write(Supplier<Collection<T>> supplier, WriteSheet writeSheet, WriteTable writeTable) {
        excelWriter.write(supplier.get(), writeSheet, writeTable);
    }

    /**
     * 填充数据到指定工作表。
     *
     * @param data       填充数据
     * @param writeSheet 工作表
     */
    public void fill(Object data, WriteSheet writeSheet) {
        excelWriter.fill(data, writeSheet);
    }

    /**
     * 按填充配置填充数据到指定工作表。
     *
     * @param data       填充数据
     * @param fillConfig 填充配置
     * @param writeSheet 工作表
     */
    public void fill(Object data, FillConfig fillConfig, WriteSheet writeSheet) {
        excelWriter.fill(data, fillConfig, writeSheet);
    }

    /**
     * 通过数据提供器填充数据到指定工作表。
     *
     * @param supplier   数据提供器
     * @param writeSheet 工作表
     */
    public void fill(Supplier<Object> supplier, WriteSheet writeSheet) {
        excelWriter.fill(supplier, writeSheet);
    }

    /**
     * 通过数据提供器按填充配置填充数据到指定工作表。
     *
     * @param supplier   数据提供器
     * @param fillConfig 填充配置
     * @param writeSheet 工作表
     */
    public void fill(Supplier<Object> supplier, FillConfig fillConfig, WriteSheet writeSheet) {
        excelWriter.fill(supplier, fillConfig, writeSheet);
    }

    /**
     * 获取写出上下文。
     *
     * @return 写出上下文
     */
    public WriteContext writeContext() {
        return excelWriter.writeContext();
    }

    /**
     * 创建一个 ExcelWriterWrapper
     *
     * @param excelWriter ExcelWriter
     * @return ExcelWriterWrapper
     */
    public static  <T> ExcelWriterWrapper<T> of(ExcelWriter excelWriter) {
        return new ExcelWriterWrapper<>(excelWriter);
    }

    // -------------------------------- sheet start

    /**
     * 创建工作表。
     *
     * @param sheetNo   工作表编号
     * @param sheetName 工作表名称
     * @return 工作表
     */
    public static WriteSheet buildSheet(Integer sheetNo, String sheetName) {
        return sheetBuilder(sheetNo, sheetName).build();
    }

    /**
     * 创建工作表。
     *
     * @param sheetNo 工作表编号
     * @return 工作表
     */
    public static WriteSheet buildSheet(Integer sheetNo) {
        return sheetBuilder(sheetNo).build();
    }

    /**
     * 创建工作表。
     *
     * @param sheetName 工作表名称
     * @return 工作表
     */
    public static WriteSheet buildSheet(String sheetName) {
        return sheetBuilder(sheetName).build();
    }

    /**
     * 创建工作表。
     *
     * @return 工作表
     */
    public static WriteSheet buildSheet() {
        return sheetBuilder().build();
    }

    /**
     * 创建工作表构造器。
     *
     * @param sheetNo   工作表编号
     * @param sheetName 工作表名称
     * @return 工作表构造器
     */
    public static ExcelWriterSheetBuilder sheetBuilder(Integer sheetNo, String sheetName) {
        return FesodSheet.writerSheet(sheetNo, sheetName);
    }

    /**
     * 创建工作表构造器。
     *
     * @param sheetNo 工作表编号
     * @return 工作表构造器
     */
    public static ExcelWriterSheetBuilder sheetBuilder(Integer sheetNo) {
        return FesodSheet.writerSheet(sheetNo);
    }

    /**
     * 创建工作表构造器。
     *
     * @param sheetName 工作表名称
     * @return 工作表构造器
     */
    public static ExcelWriterSheetBuilder sheetBuilder(String sheetName) {
        return FesodSheet.writerSheet(sheetName);
    }

    /**
     * 创建工作表构造器。
     *
     * @return 工作表构造器
     */
    public static ExcelWriterSheetBuilder sheetBuilder() {
        return FesodSheet.writerSheet();
    }

    // -------------------------------- sheet end

    // -------------------------------- table start

    /**
     * 创建表格。
     *
     * @param tableNo 表格编号
     * @return 表格
     */
    public static WriteTable buildTable(Integer tableNo) {
        return tableBuilder(tableNo).build();
    }

    /**
     * 创建表格。
     *
     * @return 表格
     */
    public static WriteTable buildTable() {
        return tableBuilder().build();
    }

    /**
     * 创建表格构造器。
     *
     * @param tableNo 表格编号
     * @return 表格构造器
     */
    public static ExcelWriterTableBuilder tableBuilder(Integer tableNo) {
        return FesodSheet.writerTable(tableNo);
    }

    /**
     * 创建表格构造器。
     *
     * @return 表格构造器
     */
    public static ExcelWriterTableBuilder tableBuilder() {
        return FesodSheet.writerTable();
    }

    // -------------------------------- table end

}
