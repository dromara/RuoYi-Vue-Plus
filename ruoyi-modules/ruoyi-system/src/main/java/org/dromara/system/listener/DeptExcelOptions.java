package org.dromara.system.listener;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.excel.core.ExcelOptionsProvider;
import org.dromara.system.service.ISysDeptService;

import java.util.Set;

/**
 * Excel 部门下拉选项数据源
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
public class DeptExcelOptions implements ExcelOptionsProvider {

    /**
     * 获取下拉选项数据
     *
     * @return 下拉选项列表
     */
    @Override
    public Set<String> getOptions() {
        ISysDeptService deptService = SpringUtils.getBean(ISysDeptService.class);
        return DeptExcelConverter.buildDeptPathMap(deptService).keySet();
    }

}
