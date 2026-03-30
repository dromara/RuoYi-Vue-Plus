package org.dromara.system.listener;

import cn.hutool.core.lang.tree.Tree;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.TreeBuildUtils;
import org.dromara.common.excel.core.ExcelOptionsProvider;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.service.ISysDeptService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
        List<Tree<Long>> trees = deptService.selectDeptTreeList(new SysDeptBo());
        Map<String, Tree<Long>> treeMap = TreeBuildUtils.buildTreeNodeMap(trees, "/", Tree::getName);
        return treeMap.keySet();
    }

}
