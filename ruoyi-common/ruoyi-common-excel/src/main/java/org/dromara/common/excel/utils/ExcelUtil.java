package org.dromara.common.excel.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterSheetBuilder;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.apache.fesod.sheet.write.metadata.fill.FillWrapper;
import org.apache.fesod.sheet.write.style.column.LongestMatchColumnWidthStyleStrategy;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.excel.convert.ExcelBigNumberConvert;
import org.dromara.common.excel.core.*;
import org.dromara.common.excel.handler.DataWriteHandler;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Excel相关处理
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelUtil {

    /**
     * 读取Excel并返回对象集合
     *
     * @param is    文件流
     * @param clazz 接收实体类
     * @return 数据列表
     */
    public static <T> List<T> importExcel(InputStream is, Class<T> clazz) {
        return FesodSheet.read(is).head(clazz).autoCloseStream(false).sheet().doReadSync();
    }

    /**
     * 读取Excel并返回解析结果（带默认校验功能）
     *
     * @param is         文件流
     * @param clazz      接收实体类
     * @param isValidate 是否开启校验
     * @return 解析结果（含成功数据、错误信息）
     */
    public static <T> ExcelResult<T> importExcel(InputStream is, Class<T> clazz, boolean isValidate) {
        DefaultExcelListener<T> listener = new DefaultExcelListener<>(isValidate);
        FesodSheet.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * 读取Excel并返回解析结果（使用自定义监听器）
     *
     * @param is       文件流
     * @param clazz    接收实体类
     * @param listener 自定义监听器
     * @return 解析结果（含成功数据、错误信息）
     */
    public static <T> ExcelResult<T> importExcel(InputStream is, Class<T> clazz, ExcelListener<T> listener) {
        FesodSheet.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param response  响应体
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            exportExcel(list, sheetName, clazz, false, os, null);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 大数据量Excel导出
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     */
    public static <T> void exportExcelZip(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response) {
        exportExcelZip(list, sheetName, clazz, response, 999, 5, 10);
    }

    /**
     * 大数据量Excel导出
     *
     * @param list       导出数据集合
     * @param sheetName  工作表的名称
     * @param clazz      实体类
     * @param pageSize   每页条数
     * @param coreThread 核心线程
     * @param maxThread  最大线程
     */
    public static <T> void exportExcelZip(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response, int pageSize, int coreThread, int maxThread) {
        try {
            List<List<T>> pageList = ListUtil.partition(list, pageSize);
            int totalPage = pageList.size();

            // 数据量不足1页，直接导出普通Excel
            if (totalPage == 1) {
                resetResponse(sheetName, response);
                ServletOutputStream os = response.getOutputStream();
                exportExcel(list, sheetName, clazz, false, os, null);
                return;
            }

            // ====================== 初始化线程池 ======================
            ExecutorService executor = ThreadUtil.newExecutor(coreThread, maxThread);

            // 存储结果：key=文件名，value=文件字节码（线程安全Map）
            Map<String, byte[]> excelMap = new ConcurrentSkipListMap<>();

            // 计数器：等待所有线程完成
            CountDownLatch latch = new CountDownLatch(totalPage);

            // ====================== 多线程生成Excel ======================
            for (int i = 0; i < totalPage; i++) {
                int pageNum = i + 1;
                List<T> pageData = pageList.get(i);

                executor.execute(() -> {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        String fileName = sheetName + "_第" + pageNum + "页.xlsx";
                        // 生成Excel
                        exportExcel(pageData, sheetName + "_第" + pageNum + "页", clazz, false, bos, null);
                        excelMap.put(fileName, bos.toByteArray());
                    } catch (Exception e) {
                        throw new RuntimeException("第" + pageNum + "页Excel生成失败", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待所有线程执行完毕（最多等待10分钟）
            latch.await(10, TimeUnit.MINUTES);

            // ====================== 打包ZIP并下载 ======================
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                "attachment;filename*=UTF-8''" + java.net.URLEncoder.encode(sheetName, "UTF-8") + ".zip");

            ZipOutputStream zipOut = ZipUtil.getZipOutputStream(response.getOutputStream(), CharsetUtil.CHARSET_UTF_8);

            // 写入ZIP
            for (Map.Entry<String, byte[]> entry : excelMap.entrySet()) {
                zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                zipOut.write(entry.getValue());
                zipOut.closeEntry();
            }

            // 关闭资源
            IoUtil.close(zipOut);
            executor.shutdown();

        } catch (Exception e) {
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param response  响应体
     * @param options   级联下拉选
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response, List<DropDownOptions> options) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            exportExcel(list, sheetName, clazz, false, os, options);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param merge     是否合并单元格
     * @param response  响应体
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, HttpServletResponse response) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            exportExcel(list, sheetName, clazz, merge, os, null);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param merge     是否合并单元格
     * @param response  响应体
     * @param options   级联下拉选
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, HttpServletResponse response, List<DropDownOptions> options) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            exportExcel(list, sheetName, clazz, merge, os, options);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param os        输出流
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, OutputStream os) {
        exportExcel(list, sheetName, clazz, false, os, null);
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param os        输出流
     * @param options   级联下拉选内容
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, OutputStream os, List<DropDownOptions> options) {
        exportExcel(list, sheetName, clazz, false, os, options);
    }

    /**
     * 导出excel
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param clazz     实体类
     * @param merge     是否合并单元格
     * @param os        输出流
     */
    public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge,
                                       OutputStream os, List<DropDownOptions> options) {
        ExcelWriterSheetBuilder builder = FesodSheet.write(os, clazz)
            .autoCloseStream(false)
            // 自动适配
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
            // 大数值自动转换 防止失真
            .registerConverter(new ExcelBigNumberConvert())
            .registerWriteHandler(new DataWriteHandler(clazz))
            .sheet(sheetName);
        if (merge) {
            // 合并处理器
            builder.registerWriteHandler(new CellMergeStrategy(list, true));
        }
        // 添加下拉框操作
        builder.registerWriteHandler(new ExcelDownHandler(options));
        builder.doWrite(list);
    }

    /**
     * 导出excel
     *
     * @param headType 带Excel注解的类型
     * @param os       输出流
     * @param options  Excel下拉可选项
     * @param consumer 导出助手消费函数
     */
    public static <T> void exportExcel(Class<T> headType, OutputStream os, List<DropDownOptions> options, Consumer<ExcelWriterWrapper<T>> consumer) {
        try (ExcelWriter writer = FesodSheet.write(os, headType)
            .autoCloseStream(false)
            // 自动适配
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
            // 大数值自动转换 防止失真
            .registerConverter(new ExcelBigNumberConvert())
            // 批注必填项处理
            .registerWriteHandler(new DataWriteHandler(headType))
            // 添加下拉框操作
            .registerWriteHandler(new ExcelDownHandler(options))
            .build()) {
            // 执行消费函数
            consumer.accept(ExcelWriterWrapper.of(writer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 导出excel
     *
     * @param headType 带Excel注解的类型
     * @param os       输出流
     * @param consumer 导出助手消费函数
     */
    public static <T> void exportExcel(Class<T> headType, OutputStream os, Consumer<ExcelWriterWrapper<T>> consumer) {
        exportExcel(headType, os, null, consumer);
    }

    /**
     * 单表多数据模板导出 模板格式为 {.属性}
     *
     * @param filename     文件名
     * @param templatePath 模板路径 resource 目录下的路径包括模板文件名
     *                     例如: excel/temp.xlsx
     *                     重点: 模板文件必须放置到启动类对应的 resource 目录下
     * @param data         模板需要的数据
     * @param response     响应体
     */
    public static <T> void exportTemplate(List<T> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            if (CollUtil.isEmpty(data)) {
                throw new IllegalArgumentException("数据为空");
            }
            resetResponse(filename, response);
            ServletOutputStream os = response.getOutputStream();
            exportTemplate(data, templatePath, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 单表多数据模板导出 模板格式为 {.属性}
     *
     * @param templatePath 模板路径 resource 目录下的路径包括模板文件名
     *                     例如: excel/temp.xlsx
     *                     重点: 模板文件必须放置到启动类对应的 resource 目录下
     * @param data         模板需要的数据
     * @param os           输出流
     */
    public static <T> void exportTemplate(List<T> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = FesodSheet.write(os)
            .withTemplate(templateResource.getStream())
            .autoCloseStream(false)
            // 大数值自动转换 防止失真
            .registerConverter(new ExcelBigNumberConvert())
            .registerWriteHandler(new DataWriteHandler(data.getFirst().getClass()))
            .build();
        try {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
            // 单表多数据导出 模板格式为 {.属性}
            for (T d : data) {
                excelWriter.fill(d, fillConfig, writeSheet);
            }
        } finally {
            excelWriter.finish();
        }
    }

    /**
     * 多表多数据模板导出 模板格式为 {key.属性}
     *
     * @param filename     文件名
     * @param templatePath 模板路径 resource 目录下的路径包括模板文件名
     *                     例如: excel/temp.xlsx
     *                     重点: 模板文件必须放置到启动类对应的 resource 目录下
     * @param data         模板需要的数据
     * @param response     响应体
     */
    public static void exportTemplateMultiList(Map<String, Object> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            if (CollUtil.isEmpty(data)) {
                throw new IllegalArgumentException("数据为空");
            }
            resetResponse(filename, response);
            ServletOutputStream os = response.getOutputStream();
            exportTemplateMultiList(data, templatePath, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 多sheet模板导出 模板格式为 {key.属性}
     *
     * @param filename     文件名
     * @param templatePath 模板路径 resource 目录下的路径包括模板文件名
     *                     例如: excel/temp.xlsx
     *                     重点: 模板文件必须放置到启动类对应的 resource 目录下
     * @param data         模板需要的数据
     * @param response     响应体
     */
    public static void exportTemplateMultiSheet(List<Map<String, Object>> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            if (CollUtil.isEmpty(data)) {
                throw new IllegalArgumentException("数据为空");
            }
            resetResponse(filename, response);
            ServletOutputStream os = response.getOutputStream();
            exportTemplateMultiSheet(data, templatePath, os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 多表多数据模板导出 模板格式为 {key.属性}
     *
     * @param templatePath 模板路径 resource 目录下的路径包括模板文件名
     *                     例如: excel/temp.xlsx
     *                     重点: 模板文件必须放置到启动类对应的 resource 目录下
     * @param data         模板需要的数据
     * @param os           输出流
     */
    public static void exportTemplateMultiList(Map<String, Object> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = FesodSheet.write(os)
            .withTemplate(templateResource.getStream())
            .autoCloseStream(false)
            // 大数值自动转换 防止失真
            .registerConverter(new ExcelBigNumberConvert())
            .build();
        try {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            for (Map.Entry<String, Object> map : data.entrySet()) {
                // 设置列表后续还有数据
                FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
                if (map.getValue() instanceof Collection) {
                    // 多表导出必须使用 FillWrapper
                    excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
                } else {
                    excelWriter.fill(map.getValue(), fillConfig, writeSheet);
                }
            }
        } finally {
            excelWriter.finish();
        }
    }

    /**
     * 多sheet模板导出 模板格式为 {key.属性}
     *
     * @param templatePath 模板路径 resource 目录下的路径包括模板文件名
     *                     例如: excel/temp.xlsx
     *                     重点: 模板文件必须放置到启动类对应的 resource 目录下
     * @param data         模板需要的数据
     * @param os           输出流
     */
    public static void exportTemplateMultiSheet(List<Map<String, Object>> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = FesodSheet.write(os)
            .withTemplate(templateResource.getStream())
            .autoCloseStream(false)
            // 大数值自动转换 防止失真
            .registerConverter(new ExcelBigNumberConvert())
            .build();
        try {
            for (int i = 0; i < data.size(); i++) {
                WriteSheet writeSheet = FesodSheet.writerSheet(i).build();
                for (Map.Entry<String, Object> map : data.get(i).entrySet()) {
                    // 设置列表后续还有数据
                    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
                    if (map.getValue() instanceof Collection) {
                        // 多表导出必须使用 FillWrapper
                        excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
                    } else {
                        excelWriter.fill(map.getValue(), writeSheet);
                    }
                }
            }
        } finally {
            excelWriter.finish();
        }
    }

    /**
     * 重置响应体
     */
    private static void resetResponse(String sheetName, HttpServletResponse response) throws UnsupportedEncodingException {
        String filename = encodingFilename(sheetName);
        FileUtils.setAttachmentResponseHeader(response, filename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @param separator     分隔符
     * @return 解析后值
     */
    public static String convertByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(StringUtils.SEPARATOR);
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(propertyValue, separator)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[0].equals(value)) {
                        propertyString.append(itemArray[1]).append(separator);
                        break;
                    }
                }
            } else {
                if (itemArray[0].equals(propertyValue)) {
                    return itemArray[1];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 反向解析值 男=0,女=1,未知=2
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @param separator     分隔符
     * @return 解析后值
     */
    public static String reverseByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(StringUtils.SEPARATOR);
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(propertyValue, separator)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[1].equals(value)) {
                        propertyString.append(itemArray[0]).append(separator);
                        break;
                    }
                }
            } else {
                if (itemArray[1].equals(propertyValue)) {
                    return itemArray[0];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 编码文件名
     */
    public static String encodingFilename(String filename) {
        return IdUtil.fastSimpleUUID() + "_" + filename + ".xlsx";
    }

}
