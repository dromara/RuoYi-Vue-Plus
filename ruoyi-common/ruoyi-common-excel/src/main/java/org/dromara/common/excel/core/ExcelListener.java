package org.dromara.common.excel.core;

import org.apache.fesod.sheet.read.listener.ReadListener;

/**
 * Excel 导入监听
 *
 * @author Lion Li
 */
public interface ExcelListener<T> extends ReadListener<T> {

    /**
     * 获取 Excel 导入结果。
     *
     * @return Excel 导入结果
     */
    ExcelResult<T> getExcelResult();

}
