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
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.TreeBuildUtils;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.service.ISysDeptService;

import java.util.HashMap;
import java.util.Map;

/**
 * Excel 部门转换处理
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
public class DeptExcelConverter implements Converter<Long> {

    /**
     * 按工作簿配置实例复用部门映射，配置实例释放后缓存可回收。
     */
    private static final Cache<GlobalConfiguration, DeptMaps> DEPT_CACHE = Caffeine.newBuilder()
        .weakKeys()
        .build();

    /**
     * 获取部门导入导出映射缓存。
     *
     * @return 部门 ID 与部门路径名称互转映射
     */
    private DeptMaps getDeptMaps(GlobalConfiguration configuration) {
        if (configuration == null) {
            return loadDeptMaps();
        }
        return DEPT_CACHE.get(configuration, key -> loadDeptMaps());
    }

    /**
     * 根据当前部门树构建导入和导出使用的双向映射。
     */
    private DeptMaps loadDeptMaps() {
        ISysDeptService deptService = SpringUtils.getBean(ISysDeptService.class);
        Map<String, Tree<Long>> departments = TreeBuildUtils.buildTreeNodeMap(
            deptService.selectDeptTreeList(new SysDeptBo()),
            "/",
            Tree::getName
        );
        Map<Long, String> idToName = new HashMap<>();
        Map<String, Long> nameToId = new HashMap<>();
        departments.forEach((name, node) -> {
            idToName.put(node.getId(), name);
            nameToId.put(name, node.getId());
        });
        return new DeptMaps(idToName, nameToId);
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
        Long deptId = getDeptMaps(globalConfiguration).nameToId().get(deptName);
        if (deptId == null) {
            throw new ServiceException("部门不存在：" + deptName);
        }
        return deptId;
    }

    /**
     * 【导出】部门 ID → 转为 Excel 显示的完整路径名称
     */
    @Override
    public WriteCellData<?> convertToExcelData(Long value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        String deptName = getDeptMaps(globalConfiguration).idToName().getOrDefault(value, "");
        return new WriteCellData<>(deptName);
    }

    /**
     * 部门导入导出映射缓存。
     *
     * @param idToName 部门 ID 到部门名称的映射
     * @param nameToId 部门名称到部门 ID 的映射
     */
    private record DeptMaps(Map<Long, String> idToName, Map<String, Long> nameToId) {
    }

}
