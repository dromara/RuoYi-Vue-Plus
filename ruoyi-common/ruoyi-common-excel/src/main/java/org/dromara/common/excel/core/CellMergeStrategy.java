package org.dromara.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.metadata.Head;
import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.merge.AbstractMergeStrategy;
import org.apache.fesod.sheet.write.metadata.holder.WriteSheetHolder;
import org.apache.fesod.sheet.write.metadata.holder.WriteWorkbookHolder;
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

    /**
     * 合并单元格区域集合。
     */
    private final List<CellRangeAddress> cellList;

    /**
     * 创建单元格合并策略。
     *
     * @param cellList 合并区域列表
     */
    public CellMergeStrategy(List<CellRangeAddress> cellList) {
        this.cellList = cellList;
    }

    /**
     * 根据数据列表创建单元格合并策略。
     *
     * @param list     数据列表
     * @param hasTitle 是否存在标题行
     */
    public CellMergeStrategy(List<?> list, boolean hasTitle) {
        this.cellList = CellMergeHandler.of(hasTitle).handle(list);
    }

    /**
     * 根据数据列表和起始行创建单元格合并策略。
     *
     * @param list     数据列表
     * @param hasTitle 是否存在标题行
     * @param rowIndex 起始行索引
     */
    public CellMergeStrategy(List<?> list, boolean hasTitle, int rowIndex) {
        this.cellList = CellMergeHandler.of(hasTitle, rowIndex).handle(list);
    }

    /**
     * 写入单元格时清理合并区域非首行内容。
     *
     * @param sheet            工作表
     * @param cell             当前单元格
     * @param head             表头
     * @param relativeRowIndex 相对行索引
     */
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

    /**
     * 工作表创建后写入合并区域。
     *
     * @param writeWorkbookHolder 工作簿上下文
     * @param writeSheetHolder    工作表上下文
     */
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
