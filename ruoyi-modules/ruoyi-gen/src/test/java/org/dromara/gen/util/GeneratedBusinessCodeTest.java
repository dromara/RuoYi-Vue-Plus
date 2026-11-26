package org.dromara.gen.util;

import cn.hutool.core.lang.Dict;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.mybatis.enums.DataBaseType;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.gen.config.properties.GenProperties;
import org.dromara.gen.domain.GenTable;
import org.dromara.gen.domain.GenTableColumn;
import org.dromara.gen.mapper.GenTableMapper;
import org.dromara.gen.mapper.GenTableColumnMapper;
import org.dromara.gen.service.GenTableServiceImpl;
import org.dromara.gen.util.template.PathNamedTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipInputStream;
import javax.tools.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class GeneratedBusinessCodeTest {
    private static final GenericApplicationContext context = new GenericApplicationContext();

    @BeforeAll
    static void configureGenerator() {
        GenProperties properties = new GenProperties();
        properties.setPackageName("org.example.billing");
        properties.setAuthor("billing");
        properties.setAutoRemovePre(true);
        properties.setTablePrefix("biz_");
        context.registerBean(GenProperties.class, () -> properties);
        AtomicLong menuIds = new AtomicLong(1760000000000000000L);
        context.registerBean(IdentifierGenerator.class, () -> entity -> menuIds.incrementAndGet());
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
        context.registerBean(DynamicRoutingDataSource.class, () -> mock(DynamicRoutingDataSource.class));
        context.refresh();
        new SpringUtils().setApplicationContext(context);
    }

    @AfterAll
    static void closeContext() {
        context.close();
    }

    @Test
    void invoiceEntityPreservesMoneyPrecisionAndUsesInheritedAuditFields() {
        GenTable table = table("crud", "vue");
        column(table, "gross_amount", "DECIMAL(18,2)");
        column(table, "created_by_user", "bigint");
        column(table, "create_by", "bigint");
        column(table, "version", "int");
        String domain = render(table, "fm/java/domain.java.ftl");
        assertThat(domain).contains("private BigDecimal grossAmount;", "private Long createdByUser;",
            "@Version", "@TableId(value = \"invoice_id\")", "private Long invoiceId;")
            .doesNotContain("private Long createBy;");
    }

    @ParameterizedTest
    @ValueSource(strings = {"vue", "react"})
    void generatedClientDistinguishesPaymentFlagsFromRecordIdentifiers(String frontend) {
        GenTable table = table("crud", frontend);
        column(table, "is_paid", "tinyint(1)");
        column(table, "customer_id", "bigint");
        column(table, "gross_amount", "decimal(18,2)");
        column(table, "version", "int");
        String types = render(table, "fm/" + frontend + "/types.ts.ftl");
        assertThat(types).contains("isPaid: boolean;", "customerId: string | number;", "grossAmount: number;")
            .doesNotContain("version?:");
    }

    @Test
    void invoiceQueryUsesNamedDateBoundsAndExcludesCurrentInvoiceFromCompositeUniquenessCheck() {
        GenTable table = table("crud", "vue");
        column(table, "invoice_name", "varchar(100)");
        column(table, "issued_at", "timestamp");
        column(table, "customer_id", "bigint");
        table.setOptions("""
            {"enableUnique":true,"uniqueFields":["customer_id","invoiceName"]}
            """);
        String service = render(table, "fm/java/serviceImpl.java.ftl");
        assertThat(service).contains(".betweenParams(Invoice::getIssuedAt, params, \"beginIssuedAt\", \"endIssuedAt\")",
            ".likeIfText(Invoice::getInvoiceName, bo.getInvoiceName())",
            "lqw.eq(Invoice::getCustomerId, bo.getCustomerId())", "lqw.eq(Invoice::getInvoiceName, bo.getInvoiceName())",
            "lqw.ne(bo.getInvoiceId() != null, Invoice::getInvoiceId, bo.getInvoiceId())");
    }

    @Test
    void addingRootOrChildUsesConfiguredRootOrSelectedRowInReactTree() {
        GenTable table = table("tree", "react");
        column(table, "parent_id", "bigint");
        column(table, "invoice_name", "varchar(80)");
        table.setOptions("""
            {"treeCode":"invoice_id","treeParentCode":"parent_id","treeName":"invoice_name","treeRootValue":"-1"}
            """);
        String page = render(table, "fm/react/index-tree.tsx.ftl");
        assertThat(page).contains("parentId: row?.invoiceId ?? -1");
    }

    @ParameterizedTest
    @CsvSource({"vue,true,views,vue", "vue,false,views,vue", "react,true,pages,tsx", "react,false,pages,tsx"})
    void downloadPackagesThePreviewedPageAndMatchingControllerAndMenu(String frontend, boolean export,
            String pages, String extension) throws IOException {
        GenTable table = table("crud", frontend);
        table.setDataName("billing");
        table.setOptions("{\"enableExport\":" + export + "}");
        column(table, "invoice_name", "varchar(100)");
        GenTableMapper tables = mock(GenTableMapper.class);
        GenTableColumnMapper columns = mock(GenTableColumnMapper.class);
        when(tables.selectById(42L)).thenReturn(table);
        when(columns.currentModelClass()).thenReturn(GenTableColumn.class);
        when(columns.lambda()).thenCallRealMethod();
        when(columns.selectList(any())).thenReturn(table.getColumns());
        var service = new GenTableServiceImpl(tables, columns);
        try (var database = mockStatic(DataBaseHelper.class)) {
            database.when(() -> DataBaseHelper.getDataBaseType("billing")).thenReturn(DataBaseType.MY_SQL);
            Map<String, String> preview = service.previewCode(42L);
            Map<String, String> files = new HashMap<>();
            try (var zip = new ZipInputStream(new ByteArrayInputStream(service.downloadCode(42L)))) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            String page = frontend + "/" + pages + "/billing/invoice/index." + extension;
            String controller = "main/java/org/example/billing/controller/InvoiceController.java";
            assertThat(files).containsKeys(page, controller, "invoiceMenu.sql", "main/resources/mapper/billing/InvoiceMapper.xml",
                frontend + "/api/billing/invoice/index.ts", frontend + "/api/billing/invoice/types.ts");
            assertThat(files.get(page)).isEqualTo(preview.get("fm/" + frontend + "/index." + extension + ".ftl"));
            for (String path : List.of(controller, "invoiceMenu.sql", page)) {
                assertThat(files.get(path)).as(path).contains("billing:invoice:edit");
                assertThat(files.get(path).contains("billing:invoice:export")).as(path + " export permission").isEqualTo(export);
            }
        }
    }

    @Test
    void hiddenPrimaryKeyIsReturnedForRowActionsWithoutAddingAnExcelColumn() {
        GenTable table = tableWithHiddenKey("vue");
        String vo = render(table, "fm/java/vo.java.ftl");
        assertThat(vo).contains("private Long invoiceId;", "@ExcelProperty(value = \"invoice_name\")")
            .doesNotContain("@ExcelProperty(value = \"invoice_id\"");
    }

    @Test
    void updatePayloadCarriesTheKeyEvenWhenUsersCannotEditIt() {
        GenTable table = tableWithHiddenKey("vue");
        String bo = render(table, "fm/java/bo.java.ftl");
        assertThat(bo).contains("private Long invoiceId;", "private String invoiceName;");
    }

    @ParameterizedTest
    @ValueSource(strings = {"vue", "react"})
    void hiddenKeyIsAvailableToRowActionsAndFormsButIsNotAQueryFilter(String frontend) {
        GenTable table = tableWithHiddenKey(frontend);
        String types = render(table, "fm/" + frontend + "/types.ts.ftl");
        assertThat(types.substring(0, types.indexOf("export interface InvoiceForm")))
            .contains("invoiceId: string | number;");
        assertThat(types.substring(types.indexOf("export interface InvoiceForm"), types.indexOf("export interface InvoiceQuery")))
            .contains("invoiceId?: string | number;");
        assertThat(types.substring(types.indexOf("export interface InvoiceQuery"))).doesNotContain("invoiceId");
    }

    @ParameterizedTest
    @CsvSource({"treeCode,树编码字段", "treeParentCode,树父编码字段", "treeName,树名称字段"})
    void treeConfigurationRejectsReferencesToRemovedColumns(String option, String label) {
        GenTable table = table("tree", "vue");
        column(table, "parent_id", "bigint");
        column(table, "invoice_name", "varchar(80)");
        table.getParams().putAll(Map.of("treeCode", "invoice_id", "treeParentCode", "parent_id", "treeName", "invoice_name"));
        var service = new GenTableServiceImpl(mock(GenTableMapper.class), mock(GenTableColumnMapper.class));
        service.validateEdit(table);

        table.getParams().put(option, "removed_column");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.validateEdit(table))
            .isInstanceOf(org.dromara.common.core.exception.ServiceException.class)
            .hasMessageContaining(label).hasMessageContaining("不存在");
    }

    @Test
    void schemaSyncKeepsConfiguredFieldOptionsAndAddsAndRemovesOnlyChangedColumns() {
        GenTable table = table("crud", "vue");
        table.setDataName("billing");
        var previous = new GenTableColumn();
        initialize(table, previous, "payment_state", "varchar(20)");
        previous.setColumnId(11L);
        previous.setIsList("0");
        previous.setIsInsert("0");
        previous.setIsEdit("0");
        previous.setQueryType("NE");
        previous.setHtmlType("select");
        previous.setDictType("payment_state");
        var obsolete = new GenTableColumn();
        initialize(table, obsolete, "old_total", "int");
        obsolete.setColumnId(12L);
        table.getPkColumn().setColumnId(10L);

        GenTableMapper tables = mock(GenTableMapper.class);
        GenTableColumnMapper columns = mock(GenTableColumnMapper.class);
        when(tables.selectById(42L)).thenReturn(table);
        when(columns.currentModelClass()).thenReturn(GenTableColumn.class);
        when(columns.lambda()).thenCallRealMethod();
        when(columns.selectList(any())).thenReturn(table.getColumns());
        var service = new GenTableServiceImpl(tables, columns);
        var metadata = mock(GenTableServiceImpl.class);
        var key = databaseColumn("invoice_id", "bigint");
        key.setIsPk("1");
        var state = databaseColumn("payment_state", "varchar(50)");
        var amount = databaseColumn("gross_amount", "decimal(18,2)");
        var liveColumns = List.of(key, state, amount);
        when(metadata.selectDbTableColumnsByName("biz_invoice", "billing")).thenReturn(liveColumns);
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(metadata);
            service.synchDb(42L);
        }
        verify(columns).insertOrUpdateBatch(liveColumns);
        verify(columns).deleteByIds(List.of(12L));
        assertThat(amount.getJavaType()).isEqualTo("BigDecimal");
        assertThat(state.getColumnType()).isEqualTo("varchar(50)");
        assertThat(state.getColumnId()).isEqualTo(11L);
        assertThat(List.of(state.isInsert(), state.isEdit(), state.isList(), state.isQuery()))
            .containsExactly(false, false, false, true);
        assertThat(state.getQueryType()).isEqualTo("NE");
        assertThat(state.getHtmlType()).isEqualTo("select");
        assertThat(state.getDictType()).isEqualTo("payment_state");
    }

    private static GenTableColumn databaseColumn(String name, String type) {
        var column = new GenTableColumn();
        column.setColumnName(name);
        column.setColumnType(type);
        return column;
    }

    @Test
    void generatedInvoiceModelCompilesWithTypedAccessToHiddenKeyAmountAndDateFilters(@TempDir Path output)
            throws IOException {
        GenTable table = tableWithHiddenKey("vue");
        column(table, "gross_amount", "decimal(18,2)");
        column(table, "issued_at", "timestamp");
        Path entity = Files.writeString(output.resolve("Invoice.java"), render(table, "fm/java/domain.java.ftl"));
        Path bo = Files.writeString(output.resolve("InvoiceBo.java"), render(table, "fm/java/bo.java.ftl"));
        Path caller = Files.writeString(output.resolve("InvoiceCaller.java"), """
            import java.math.BigDecimal;
            import java.time.LocalDateTime;
            import org.example.billing.domain.Invoice;
            import org.example.billing.domain.bo.InvoiceBo;

            class InvoiceCaller {
                void update(Invoice invoice, InvoiceBo form, LocalDateTime start) {
                    Long id = form.getInvoiceId();
                    BigDecimal amount = form.getGrossAmount();
                    invoice.setInvoiceId(id);
                    invoice.setGrossAmount(amount);
                    invoice.setIssuedAt(form.getIssuedAt());
                    form.getParams().put("beginIssuedAt", start);
                }
            }
            """);
        var diagnostics = new ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics,
            "--release", "21", "-encoding", "UTF-8", "-classpath", System.getProperty("java.class.path"),
            "-processor", "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
            "-d", output.toString(), entity.toString(), bo.toString(), caller.toString());
        assertThat(result).as(diagnostics.toString(StandardCharsets.UTF_8)).isZero();
    }

    private static GenTable tableWithHiddenKey(String frontend) {
        GenTable table = table("crud", frontend);
        GenTableColumn key = table.getPkColumn();
        key.setIsList("0");
        key.setIsInsert("0");
        key.setIsEdit("0");
        column(table, "invoice_name", "varchar(80)");
        return table;
    }

    private static GenTable table(String category, String frontend) {
        GenTable table = new GenTable();
        table.setTableId(42L);
        table.setTableName("biz_invoice");
        table.setTableComment("Invoice");
        GenUtils.initTable(table);
        table.setTplCategory(category);
        table.setFrontendType(frontend);
        table.setColumns(new ArrayList<>());
        GenTableColumn key = new GenTableColumn();
        key.setIsPk("1");
        initialize(table, key, "invoice_id", "bigint");
        table.setPkColumn(key);
        return table;
    }

    private static void column(GenTable table, String name, String type) {
        initialize(table, new GenTableColumn(), name, type);
    }

    private static void initialize(GenTable table, GenTableColumn column, String name, String type) {
        column.setColumnName(name);
        column.setColumnType(type);
        column.setColumnComment(name);
        GenUtils.initColumnField(column, table);
        table.getColumns().add(column);
    }

    private static String render(GenTable table, String path) {
        Dict binding = TemplateEngineUtils.buildContext(table);
        try (var database = mockStatic(DataBaseHelper.class)) {
            database.when(() -> DataBaseHelper.getDataBaseType("billing")).thenReturn(DataBaseType.MY_SQL);
            List<PathNamedTemplate> templates = TemplateEngineUtils.getTemplateList(table.getTplCategory(), "billing",
                table.getFrontendType());
            return templates.stream().filter(template -> path.equals(template.getPathName()))
                .findFirst().orElseThrow().render(binding);
        }
    }
}
