package org.dromara.common.excel.core;

import java.util.Set;

/**
 * Excel下拉选项数据提供接口
 *
 * @author Angus
 */
public interface ExcelOptionsProvider {

    /**
     * 获取下拉选项数据
     *
     * @return 下拉选项列表
     */
    Set<String> getOptions();

}
