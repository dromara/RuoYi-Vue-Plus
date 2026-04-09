package org.dromara.system.listener;

import cn.hutool.core.lang.tree.Tree;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.TreeBuildUtils;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.service.ISysDeptService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Excel 部门转换处理
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
public class DeptExcelConverter implements Converter<Long> {

    private static final String CACHE_KEY = "dept:excel";

    /**
     * Caffeine 缓存：key=CACHE_KEY，value=[idToName, nameToId]，5分钟过期
     */
    private static final Cache<String, DeptMaps> DEPT_CACHE = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build();

    private DeptMaps getDeptMaps() {
        ISysDeptService deptService = SpringUtils.getBean(ISysDeptService.class);
        return DEPT_CACHE.get(CACHE_KEY, k -> {
            Map<String, Tree<Long>> deptPathToTreeMap = buildDeptPathMap(deptService);
            Map<Long, String> idToName = new HashMap<>();
            Map<String, Long> nameToId = new HashMap<>();
            deptPathToTreeMap.forEach((name, treeNode) -> {
                Long deptId = treeNode.getId();
                idToName.put(deptId, name);
                nameToId.put(name, deptId);
            });
            return new DeptMaps(idToName, nameToId);
        });
    }

    /**
     * 构建部门路径 → 树节点映射，供 Converter 和 Options 共用
     */
    private Map<String, Tree<Long>> buildDeptPathMap(ISysDeptService deptService) {
        return TreeBuildUtils.buildTreeNodeMap(
            deptService.selectDeptTreeList(new SysDeptBo()),
            "/",
            Tree::getName
        );
    }

    /**
     * 指定 Java 类型：Long（部门ID）
     */
    @Override
    public Class<?> supportJavaTypeKey() {
        return Long.class;
    }

    /**
     * 指定 Excel 类型：字符串
     */
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    /**
     * 【导入】Excel 填写的部门完整路径 → 转为 ID
     */
    @Override
    public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        String deptName = cellData.getStringValue();
        if (StringUtils.isBlank(deptName)) {
            return null;
        }
        return getDeptMaps().nameToId().get(deptName);
    }

    /**
     * 【导出】部门 ID → 转为 Excel 显示的完整路径名称
     */
    @Override
    public WriteCellData<?> convertToExcelData(Long value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        String deptName = getDeptMaps().idToName().getOrDefault(value, "");
        return new WriteCellData<>(deptName);
    }

    private record DeptMaps(Map<Long, String> idToName, Map<String, Long> nameToId) {
    }

}
