package org.dromara.system.listener;

import lombok.RequiredArgsConstructor;
import org.dromara.common.excel.core.ExcelOptionsProvider;
import org.dromara.system.service.ISysDeptService;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Excel 部门下拉选项数据源
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
@Component
public class DeptExcelOptions implements ExcelOptionsProvider {

    private final ISysDeptService deptService;

    /**
     * 获取下拉选项数据
     *
     * @return 下拉选项列表
     */
    @Override
    public Set<String> getOptions() {
        return DeptExcelConverter.buildDeptPathMap(deptService).keySet();
    }

}
