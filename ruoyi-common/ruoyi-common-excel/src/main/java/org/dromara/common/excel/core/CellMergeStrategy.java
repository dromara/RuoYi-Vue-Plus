package org.dromara.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.merge.AbstractMergeStrategy;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;

/**
 * 列值重复合并策略
 *
 * @author Lion Li
 */
@Slf4j
public class CellMergeStrategy extends AbstractMergeStrategy implements SheetWriteHandler {

    private final List<CellRangeAddress> cellList;

    public CellMergeStrategy(List<CellRangeAddress> cellList) {
        this.cellList = cellList;
    }

    public CellMergeStrategy(List<?> list, boolean hasTitle) {
        this.cellList = CellMergeHandler.of(hasTitle).handle(list);
    }

    public CellMergeStrategy(List<?> list, boolean hasTitle, int rowIndex) {
        this.cellList = CellMergeHandler.of(hasTitle, rowIndex).handle(list);
    }

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (CollUtil.isEmpty(cellList)) {
            return;
        }
        // 单元格写入了,遍历合并区域,如果该Cell在区域内,但非首行,则清空
        final int rowIndex = cell.getRowIndex();
        for (CellRangeAddress cellAddresses : cellList) {
            final int firstRow = cellAddresses.getFirstRow();
            if (cellAddresses.isInRange(cell) && rowIndex != firstRow) {
                cell.setBlank();
            }
        }
    }

    @Override
    public void afterSheetCreate(final WriteWorkbookHolder writeWorkbookHolder, final WriteSheetHolder writeSheetHolder) {
        if (CollUtil.isEmpty(cellList)) {
            return;
        }
        // 在 Sheet 创建时提前写入合并区域；后续写入只会影响首格，不会移除合并
        final Sheet sheet = writeSheetHolder.getSheet();
        for (CellRangeAddress item : cellList) {
            sheet.addMergedRegion(item);
        }
    }

}
