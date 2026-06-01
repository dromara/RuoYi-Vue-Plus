package org.dromara.common.excel.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.read.builder.ExcelReaderBuilder;
import org.apache.fesod.sheet.read.builder.ExcelReaderSheetBuilder;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.builder.ExcelWriterSheetBuilder;
import org.apache.fesod.sheet.write.handler.WriteHandler;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.apache.fesod.sheet.write.metadata.fill.FillWrapper;
import org.apache.fesod.sheet.write.style.column.LongestMatchColumnWidthStyleStrategy;
import org.apache.fesod.sheet.write.style.column.SimpleColumnWidthStyleStrategy;
import org.apache.fesod.sheet.write.style.row.SimpleRowHeightStyleStrategy;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.excel.convert.ExcelBigNumberConvert;
import org.dromara.common.excel.core.CellMergeStrategy;
import org.dromara.common.excel.core.DefaultExcelListener;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.excel.core.ExcelDownHandler;
import org.dromara.common.excel.core.ExcelListener;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.handler.DataWriteHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Excel 导出构造器。
 *
 * @author Lion Li
 */
public final class ExcelBuilder<T> {

    private static final String DEFAULT_SHEET_NAME = "sheet1";

    private static final int DEFAULT_ZIP_PAGE_SIZE = 999;

    private final List<T> data;

    private final Class<T> headType;

    private String sheetName = DEFAULT_SHEET_NAME;

    private Integer sheetNo;

    private boolean merge;

    private List<DropDownOptions> options;

    private boolean zip;

    private int pageSize = DEFAULT_ZIP_PAGE_SIZE;

    private String password;

    private Boolean needHead;

    private Boolean automaticMergeHead;

    private Collection<String> includeFields;

    private Collection<String> excludeFields;

    private Collection<Integer> includeIndexes;

    private Collection<Integer> excludeIndexes;

    private Boolean orderByIncludeColumn;

    private Integer columnWidth;

    private Short headRowHeight;

    private Short contentRowHeight;

    private List<WriteHandler> writeHandlers;

    private List<Converter<?>> converters;

    private ExcelBuilder(List<T> data, Class<T> headType) {
        this.data = data;
        this.headType = headType;
    }

    /**
     * 创建导出构造器。
     *
     * @param data     导出数据
     * @param headType 表头类型
     * @return 导出构造器
     */
    public static <T> ExcelBuilder<T> of(List<T> data, Class<T> headType) {
        return new ExcelBuilder<>(data, headType);
    }

    /**
     * 创建自定义写出构造器。
     *
     * @param headType 表头类型
     * @return 导出构造器
     */
    public static <T> ExcelBuilder<T> writer(Class<T> headType) {
        return new ExcelBuilder<>(null, headType);
    }

    /**
     * 创建模板导出构造器。
     *
     * @param templatePath 模板路径
     * @return 模板导出构造器
     */
    public static TemplateBuilder template(String templatePath) {
        return new TemplateBuilder(templatePath);
    }

    /**
     * 创建导入读取构造器。
     *
     * @param is    文件流
     * @param clazz 接收实体类
     * @return 导入读取构造器
     */
    public static <T> ReadBuilder<T> read(InputStream is, Class<T> clazz) {
        return new ReadBuilder<>(is, clazz);
    }

    /**
     * 设置工作表名称。
     */
    public ExcelBuilder<T> sheetName(String sheetName) {
        this.sheetName = StringUtils.blankToDefault(sheetName, DEFAULT_SHEET_NAME);
        return this;
    }

    /**
     * 设置工作表编号。
     */
    public ExcelBuilder<T> sheetNo(Integer sheetNo) {
        this.sheetNo = sheetNo;
        return this;
    }

    /**
     * 开启单元格合并。
     */
    public ExcelBuilder<T> merge() {
        return merge(true);
    }

    /**
     * 设置是否合并单元格。
     */
    public ExcelBuilder<T> merge(boolean merge) {
        this.merge = merge;
        return this;
    }

    /**
     * 设置下拉选项。
     */
    public ExcelBuilder<T> options(List<DropDownOptions> options) {
        this.options = options;
        return this;
    }

    /**
     * 设置导出文件密码。
     */
    public ExcelBuilder<T> password(String password) {
        this.password = password;
        return this;
    }

    /**
     * 设置是否写出表头。
     */
    public ExcelBuilder<T> needHead(boolean needHead) {
        this.needHead = needHead;
        return this;
    }

    /**
     * 设置是否自动合并多级表头。
     */
    public ExcelBuilder<T> automaticMergeHead(boolean automaticMergeHead) {
        this.automaticMergeHead = automaticMergeHead;
        return this;
    }

    /**
     * 仅导出指定字段。
     */
    public ExcelBuilder<T> includeFields(Collection<String> includeFields) {
        this.includeFields = includeFields;
        return this;
    }

    /**
     * 排除指定字段。
     */
    public ExcelBuilder<T> excludeFields(Collection<String> excludeFields) {
        this.excludeFields = excludeFields;
        return this;
    }

    /**
     * 仅导出指定列索引。
     */
    public ExcelBuilder<T> includeIndexes(Collection<Integer> includeIndexes) {
        this.includeIndexes = includeIndexes;
        return this;
    }

    /**
     * 排除指定列索引。
     */
    public ExcelBuilder<T> excludeIndexes(Collection<Integer> excludeIndexes) {
        this.excludeIndexes = excludeIndexes;
        return this;
    }

    /**
     * 设置是否按 include 列顺序导出。
     */
    public ExcelBuilder<T> orderByIncludeColumn(boolean orderByIncludeColumn) {
        this.orderByIncludeColumn = orderByIncludeColumn;
        return this;
    }

    /**
     * 设置固定列宽。
     */
    public ExcelBuilder<T> columnWidth(Integer columnWidth) {
        if (columnWidth != null && columnWidth <= 0) {
            throw new IllegalArgumentException("columnWidth 必须大于 0");
        }
        this.columnWidth = columnWidth;
        return this;
    }

    /**
     * 设置固定行高。
     */
    public ExcelBuilder<T> rowHeight(short headRowHeight, short contentRowHeight) {
        if (headRowHeight <= 0 || contentRowHeight <= 0) {
            throw new IllegalArgumentException("rowHeight 必须大于 0");
        }
        this.headRowHeight = headRowHeight;
        this.contentRowHeight = contentRowHeight;
        return this;
    }

    /**
     * 注册自定义写处理器。
     */
    public ExcelBuilder<T> registerWriteHandler(WriteHandler writeHandler) {
        if (writeHandler == null) {
            return this;
        }
        if (writeHandlers == null) {
            writeHandlers = new ArrayList<>();
        }
        writeHandlers.add(writeHandler);
        return this;
    }

    /**
     * 注册自定义转换器。
     */
    public ExcelBuilder<T> registerConverter(Converter<?> converter) {
        if (converter == null) {
            return this;
        }
        if (converters == null) {
            converters = new ArrayList<>();
        }
        converters.add(converter);
        return this;
    }

    /**
     * 开启 ZIP 分页导出。
     */
    public ExcelBuilder<T> zip() {
        return zip(DEFAULT_ZIP_PAGE_SIZE);
    }

    /**
     * 开启 ZIP 分页导出。
     *
     * @param pageSize 每个 Excel 文件的数据量
     */
    public ExcelBuilder<T> zip(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize 必须大于 0");
        }
        this.zip = true;
        this.pageSize = pageSize;
        return this;
    }

    /**
     * 写入 HTTP 响应。
     */
    public void toResponse(HttpServletResponse response) {
        if (zip) {
            exportZipToResponse(response);
            return;
        }
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            writeSheet(os);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    /**
     * 写入输出流。
     */
    public void toStream(OutputStream outputStream) {
        if (zip) {
            throw new UnsupportedOperationException("ZIP导出请使用 toResponse(HttpServletResponse)");
        }
        writeSheet(outputStream);
    }

    /**
     * 使用自定义写出逻辑写入输出流。
     */
    public void toStream(OutputStream outputStream, Consumer<ExcelWriterWrapper<T>> consumer) {
        try (ExcelWriter writer = createWriter(outputStream)) {
            consumer.accept(ExcelWriterWrapper.of(writer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用自定义写出逻辑写入 HTTP 响应。
     */
    public void toResponse(HttpServletResponse response, Consumer<ExcelWriterWrapper<T>> consumer) {
        try {
            resetResponse(sheetName, response);
            toStream(response.getOutputStream(), consumer);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel异常", e);
        }
    }

    private void writeSheet(OutputStream outputStream) {
        ExcelWriterSheetBuilder builder = createSheetBuilder(createWriterBuilder(outputStream));
        if (merge) {
            // 合并处理器
            builder.registerWriteHandler(new CellMergeStrategy(data, true));
        }
        builder.doWrite(data);
    }

    private ExcelWriter createWriter(OutputStream outputStream) {
        return createWriterBuilder(outputStream).build();
    }

    private ExcelWriterBuilder createWriterBuilder(OutputStream outputStream) {
        ExcelWriterBuilder builder = FesodSheet.write(outputStream, headType)
            .autoCloseStream(false)
            // 大数值自动转换 防止失真
            .registerConverter(new ExcelBigNumberConvert())
            // 批注必填项处理
            .registerWriteHandler(new DataWriteHandler(headType));
        if (columnWidth == null) {
            // 自动适配
            builder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        } else {
            builder.registerWriteHandler(new SimpleColumnWidthStyleStrategy(columnWidth));
        }
        if (headRowHeight != null || contentRowHeight != null) {
            builder.registerWriteHandler(new SimpleRowHeightStyleStrategy(headRowHeight, contentRowHeight));
        }
        // 添加下拉框操作
        builder.registerWriteHandler(new ExcelDownHandler(options));
        if (StringUtils.isNotBlank(password)) {
            builder.password(password);
        }
        if (needHead != null) {
            builder.needHead(needHead);
        }
        if (automaticMergeHead != null) {
            builder.automaticMergeHead(automaticMergeHead);
        }
        if (CollUtil.isNotEmpty(includeFields)) {
            builder.includeColumnFieldNames(includeFields);
        }
        if (CollUtil.isNotEmpty(excludeFields)) {
            builder.excludeColumnFieldNames(excludeFields);
        }
        if (CollUtil.isNotEmpty(includeIndexes)) {
            builder.includeColumnIndexes(includeIndexes);
        }
        if (CollUtil.isNotEmpty(excludeIndexes)) {
            builder.excludeColumnIndexes(excludeIndexes);
        }
        if (orderByIncludeColumn != null) {
            builder.orderByIncludeColumn(orderByIncludeColumn);
        }
        if (CollUtil.isNotEmpty(converters)) {
            converters.forEach(builder::registerConverter);
        }
        if (CollUtil.isNotEmpty(writeHandlers)) {
            writeHandlers.forEach(builder::registerWriteHandler);
        }
        return builder;
    }

    private ExcelWriterSheetBuilder createSheetBuilder(ExcelWriterBuilder builder) {
        if (sheetNo != null) {
            return builder.sheet(sheetNo, sheetName);
        }
        return builder.sheet(sheetName);
    }

    private void exportZipToResponse(HttpServletResponse response) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize 必须大于 0");
        }
        List<List<T>> pageList = ListUtil.partition(data, pageSize);
        if (pageList.size() <= 1) {
            zip = false;
            toResponse(response);
            return;
        }
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                "attachment;filename*=UTF-8''" + URLEncoder.encode(sheetName, StandardCharsets.UTF_8) + ".zip");

            try (ZipOutputStream zipOut = ZipUtil.getZipOutputStream(response.getOutputStream(), CharsetUtil.CHARSET_UTF_8)) {
                for (int i = 0; i < pageList.size(); i++) {
                    int pageNum = i + 1;
                    String exportSheetName = sheetName + "_第" + pageNum + "页";
                    byte[] bytes = buildZipEntry(pageList.get(i), exportSheetName);
                    zipOut.putNextEntry(new ZipEntry(exportSheetName + ".xlsx"));
                    zipOut.write(bytes);
                    zipOut.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("导出Zip异常", e);
        }
    }

    private byte[] buildZipEntry(List<T> pageData, String exportSheetName) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelBuilder<T> builder = ExcelBuilder.of(pageData, headType)
                .sheetName(exportSheetName);
            copyOptionsTo(builder);
            builder.toStream(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(exportSheetName + "Excel生成失败", e);
        }
    }

    private void copyOptionsTo(ExcelBuilder<T> builder) {
        builder.merge = merge;
        builder.options = options;
        builder.password = password;
        builder.needHead = needHead;
        builder.automaticMergeHead = automaticMergeHead;
        builder.includeFields = includeFields;
        builder.excludeFields = excludeFields;
        builder.includeIndexes = includeIndexes;
        builder.excludeIndexes = excludeIndexes;
        builder.orderByIncludeColumn = orderByIncludeColumn;
        builder.columnWidth = columnWidth;
        builder.headRowHeight = headRowHeight;
        builder.contentRowHeight = contentRowHeight;
        builder.writeHandlers = writeHandlers;
        builder.converters = converters;
    }

    /**
     * 重置响应体。
     */
    private static void resetResponse(String filename, HttpServletResponse response) throws UnsupportedEncodingException {
        FileUtils.setAttachmentResponseHeader(response, encodingFilename(filename));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
    }

    private static String encodingFilename(String filename) {
        return IdUtil.fastSimpleUUID() + "_" + filename + ".xlsx";
    }

    /**
     * Excel 导入读取构造器。
     */
    public static final class ReadBuilder<T> {

        private final InputStream inputStream;

        private final Class<T> headType;

        private boolean validate = true;

        private boolean failFast = true;

        private ExcelListener<T> listener;

        private Integer sheetNo;

        private String sheetName;

        private Integer headRowNumber;

        private Boolean ignoreEmptyRow;

        private String password;

        private Boolean autoTrim;

        private Boolean autoStrip;

        private Integer numRows;

        private List<Converter<?>> converters;

        private ReadBuilder(InputStream inputStream, Class<T> headType) {
            this.inputStream = inputStream;
            this.headType = headType;
        }

        /**
         * 设置是否校验导入数据。
         */
        public ReadBuilder<T> validate(boolean validate) {
            this.validate = validate;
            return this;
        }

        /**
         * 设置解析异常时是否立即终止读取。
         */
        public ReadBuilder<T> failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        /**
         * 设置自定义导入监听器。
         */
        public ReadBuilder<T> listener(ExcelListener<T> listener) {
            this.listener = listener;
            return this;
        }

        /**
         * 设置读取的工作表编号。
         */
        public ReadBuilder<T> sheetNo(Integer sheetNo) {
            this.sheetNo = sheetNo;
            return this;
        }

        /**
         * 设置读取的工作表名称。
         */
        public ReadBuilder<T> sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        /**
         * 设置表头行数。
         */
        public ReadBuilder<T> headRowNumber(Integer headRowNumber) {
            if (headRowNumber != null && headRowNumber < 0) {
                throw new IllegalArgumentException("headRowNumber 不能小于 0");
            }
            this.headRowNumber = headRowNumber;
            return this;
        }

        /**
         * 设置是否忽略空行。
         */
        public ReadBuilder<T> ignoreEmptyRow(boolean ignoreEmptyRow) {
            this.ignoreEmptyRow = ignoreEmptyRow;
            return this;
        }

        /**
         * 设置读取文件密码。
         */
        public ReadBuilder<T> password(String password) {
            this.password = password;
            return this;
        }

        /**
         * 设置是否自动 trim 字符串。
         */
        public ReadBuilder<T> autoTrim(boolean autoTrim) {
            this.autoTrim = autoTrim;
            return this;
        }

        /**
         * 设置是否自动 strip 字符串。
         */
        public ReadBuilder<T> autoStrip(boolean autoStrip) {
            this.autoStrip = autoStrip;
            return this;
        }

        /**
         * 设置最多读取行数。
         */
        public ReadBuilder<T> numRows(Integer numRows) {
            if (numRows != null && numRows <= 0) {
                throw new IllegalArgumentException("numRows 必须大于 0");
            }
            this.numRows = numRows;
            return this;
        }

        /**
         * 注册自定义转换器。
         */
        public ReadBuilder<T> registerConverter(Converter<?> converter) {
            if (converter == null) {
                return this;
            }
            if (converters == null) {
                converters = new ArrayList<>();
            }
            converters.add(converter);
            return this;
        }

        /**
         * 读取Excel并返回对象集合。
         */
        public List<T> doReadSync() {
            return createSheetBuilder(createReaderBuilder(null)).doReadSync();
        }

        /**
         * 读取Excel并返回解析结果。
         */
        public ExcelResult<T> doRead() {
            ExcelListener<T> readListener = listener;
            if (readListener == null) {
                readListener = new DefaultExcelListener<>(validate, failFast);
            }
            createSheetBuilder(createReaderBuilder(readListener)).doRead();
            return readListener.getExcelResult();
        }

        /**
         * 读取所有工作表并返回解析结果。
         */
        public ExcelResult<T> doReadAll() {
            ExcelListener<T> readListener = listener;
            if (readListener == null) {
                readListener = new DefaultExcelListener<>(validate, failFast);
            }
            createReaderBuilder(readListener).doReadAll();
            return readListener.getExcelResult();
        }

        /**
         * 读取所有工作表并返回对象集合。
         */
        public List<T> doReadAllSync() {
            return createReaderBuilder(null).doReadAllSync();
        }

        private ExcelReaderBuilder createReaderBuilder(ExcelListener<T> readListener) {
            ExcelReaderBuilder builder = FesodSheet.read(inputStream)
                .head(headType)
                .autoCloseStream(false);
            if (readListener != null) {
                builder.registerReadListener(readListener);
            }
            if (headRowNumber != null) {
                builder.headRowNumber(headRowNumber);
            }
            if (ignoreEmptyRow != null) {
                builder.ignoreEmptyRow(ignoreEmptyRow);
            }
            if (StringUtils.isNotBlank(password)) {
                builder.password(password);
            }
            if (autoTrim != null) {
                builder.autoTrim(autoTrim);
            }
            if (autoStrip != null) {
                builder.autoStrip(autoStrip);
            }
            if (numRows != null) {
                builder.numRows(numRows);
            }
            if (CollUtil.isNotEmpty(converters)) {
                converters.forEach(builder::registerConverter);
            }
            return builder;
        }

        private ExcelReaderSheetBuilder createSheetBuilder(ExcelReaderBuilder builder) {
            ExcelReaderSheetBuilder sheetBuilder;
            if (sheetNo != null && StringUtils.isNotBlank(sheetName)) {
                sheetBuilder = builder.sheet(sheetNo, sheetName);
            } else if (sheetNo != null) {
                sheetBuilder = builder.sheet(sheetNo);
            } else if (StringUtils.isNotBlank(sheetName)) {
                sheetBuilder = builder.sheet(sheetName);
            } else {
                sheetBuilder = builder.sheet();
            }
            if (numRows != null) {
                sheetBuilder.numRows(numRows);
            }
            return sheetBuilder;
        }
    }

    /**
     * Excel 模板导出构造器。
     */
    public static final class TemplateBuilder {

        private final String templatePath;

        private String filename = DEFAULT_SHEET_NAME;

        private TemplateMode mode;

        private Object data;

        private TemplateBuilder(String templatePath) {
            this.templatePath = templatePath;
        }

        /**
         * 设置下载文件名。
         */
        public TemplateBuilder filename(String filename) {
            this.filename = StringUtils.blankToDefault(filename, DEFAULT_SHEET_NAME);
            return this;
        }

        /**
         * 设置单表多数据模板数据，模板格式为 {.属性}。
         */
        public <T> TemplateBuilder data(List<T> data) {
            this.mode = TemplateMode.LIST;
            this.data = data;
            return this;
        }

        /**
         * 设置多表多数据模板数据，模板格式为 {key.属性}。
         */
        public TemplateBuilder multiList(Map<String, Object> data) {
            this.mode = TemplateMode.MULTI_LIST;
            this.data = data;
            return this;
        }

        /**
         * 设置多 sheet 模板数据，模板格式为 {key.属性}。
         */
        public TemplateBuilder multiSheet(List<Map<String, Object>> data) {
            this.mode = TemplateMode.MULTI_SHEET;
            this.data = data;
            return this;
        }

        /**
         * 写入 HTTP 响应。
         */
        public void toResponse(HttpServletResponse response) {
            try {
                validateData();
                resetResponse(filename, response);
                ServletOutputStream os = response.getOutputStream();
                toStream(os);
            } catch (IOException e) {
                throw new RuntimeException("导出Excel异常", e);
            }
        }

        /**
         * 写入输出流。
         */
        public void toStream(OutputStream outputStream) {
            validateData();
            ClassPathResource templateResource = new ClassPathResource(templatePath);
            try (InputStream templateStream = templateResource.getStream()) {
                ExcelWriter excelWriter = FesodSheet.write(outputStream)
                    .withTemplate(templateStream)
                    .autoCloseStream(false)
                    // 大数值自动转换 防止失真
                    .registerConverter(new ExcelBigNumberConvert())
                    .build();
                try {
                    fill(excelWriter);
                } finally {
                    excelWriter.finish();
                }
            } catch (IOException e) {
                throw new RuntimeException("读取Excel模板异常", e);
            }
        }

        @SuppressWarnings("unchecked")
        private void fill(ExcelWriter excelWriter) {
            if (mode == TemplateMode.LIST) {
                WriteSheet writeSheet = FesodSheet.writerSheet().build();
                FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
                for (Object item : (List<?>) data) {
                    excelWriter.fill(item, fillConfig, writeSheet);
                }
                return;
            }
            if (mode == TemplateMode.MULTI_LIST) {
                WriteSheet writeSheet = FesodSheet.writerSheet().build();
                for (Map.Entry<String, Object> map : ((Map<String, Object>) data).entrySet()) {
                    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
                    if (map.getValue() instanceof Collection) {
                        excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
                    } else {
                        excelWriter.fill(map.getValue(), fillConfig, writeSheet);
                    }
                }
                return;
            }
            List<Map<String, Object>> sheetData = (List<Map<String, Object>>) data;
            for (int i = 0; i < sheetData.size(); i++) {
                WriteSheet writeSheet = FesodSheet.writerSheet(i).build();
                for (Map.Entry<String, Object> map : sheetData.get(i).entrySet()) {
                    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
                    if (map.getValue() instanceof Collection) {
                        excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
                    } else {
                        excelWriter.fill(map.getValue(), writeSheet);
                    }
                }
            }
        }

        private void validateData() {
            if (mode == null || data == null) {
                throw new IllegalArgumentException("数据为空");
            }
            if (data instanceof Collection<?> collection && CollUtil.isEmpty(collection)) {
                throw new IllegalArgumentException("数据为空");
            }
            if (data instanceof Map<?, ?> map && CollUtil.isEmpty(map)) {
                throw new IllegalArgumentException("数据为空");
            }
        }

        /**
         * 模板写入模式。
         */
        private enum TemplateMode {
            /**
             * 单列表模板。
             */
            LIST,

            /**
             * 多列表模板。
             */
            MULTI_LIST,

            /**
             * 多 sheet 模板。
             */
            MULTI_SHEET
        }
    }
}
