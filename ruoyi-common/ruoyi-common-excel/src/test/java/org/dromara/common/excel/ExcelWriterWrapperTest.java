package org.dromara.common.excel;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.context.WriteContext;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.WriteTable;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.dromara.common.excel.utils.ExcelWriterWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ExcelWriterWrapper 契约单元测试")
class ExcelWriterWrapperTest {

    /**
     * 验证集合写出和 Supplier 写出都完整委托给 ExcelWriter，并在 Supplier 重载中只求值一次。
     */
    @Test
    @DisplayName("委托四种集合写出")
    void shouldDelegateCollectionWrites() {
        ExcelWriter writer = mock(ExcelWriter.class);
        ExcelWriterWrapper<String> wrapper = ExcelWriterWrapper.of(writer);
        WriteSheet sheet = ExcelWriterWrapper.buildSheet(2, "users");
        WriteTable table = ExcelWriterWrapper.buildTable(3);
        List<String> data = List.of("a", "b");
        AtomicBoolean supplied = new AtomicBoolean();
        Supplier<Collection<String>> supplier = () -> {
            supplied.set(true);
            return data;
        };

        wrapper.write(data, sheet);
        wrapper.write(supplier, sheet);
        wrapper.write(data, sheet, table);
        wrapper.write(supplier, sheet, table);

        assertTrue(supplied.get());
        verify(writer, times(2)).write(data, sheet);
        verify(writer, times(2)).write(data, sheet, table);
    }

    /**
     * 验证普通对象、填充配置和 Supplier 三类填充方法保持底层参数及 Supplier 实例不变。
     */
    @Test
    @DisplayName("委托四种模板填充")
    void shouldDelegateFillOperations() {
        ExcelWriter writer = mock(ExcelWriter.class);
        ExcelWriterWrapper<String> wrapper = ExcelWriterWrapper.of(writer);
        WriteSheet sheet = ExcelWriterWrapper.buildSheet("report");
        FillConfig config = FillConfig.builder().forceNewRow(true).build();
        Supplier<Object> supplier = () -> "value";

        wrapper.fill("value", sheet);
        wrapper.fill("value", config, sheet);
        wrapper.fill(supplier, sheet);
        wrapper.fill(supplier, config, sheet);

        verify(writer).fill("value", sheet);
        verify(writer).fill("value", config, sheet);
        verify(writer).fill(supplier, sheet);
        verify(writer).fill(supplier, config, sheet);
    }

    /**
     * 验证写出上下文和所有静态构造器均返回底层对象或包含请求元数据的 Fesod 对象。
     */
    @Test
    @DisplayName("获取上下文并构造工作表和表格")
    void shouldExposeContextAndBuildMetadata() {
        ExcelWriter writer = mock(ExcelWriter.class);
        WriteContext writeContext = mock(WriteContext.class);
        when(writer.writeContext()).thenReturn(writeContext);
        ExcelWriterWrapper<String> wrapper = ExcelWriterWrapper.of(writer);

        assertSame(writer, wrapper.excelWriter());
        assertSame(writeContext, wrapper.writeContext());
        assertEquals(4, ExcelWriterWrapper.buildSheet(4, "named").getSheetNo());
        assertEquals("named", ExcelWriterWrapper.buildSheet(4, "named").getSheetName());
        assertEquals(5, ExcelWriterWrapper.buildSheet(5).getSheetNo());
        assertEquals("report", ExcelWriterWrapper.buildSheet("report").getSheetName());
        assertNotNull(ExcelWriterWrapper.buildSheet());
        assertEquals(6, ExcelWriterWrapper.buildTable(6).getTableNo());
        assertNotNull(ExcelWriterWrapper.buildTable());
    }
}
