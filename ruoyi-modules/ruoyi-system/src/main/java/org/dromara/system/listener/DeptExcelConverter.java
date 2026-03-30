package org.dromara.system.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Excel 部门转换处理
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
@Component
public class DeptExcelConverter implements Converter<Long> {

    /**
     * 线程内缓存：ID → 名称
     */
    private static final ThreadLocal<Map<Long, String>> TL_ID_TO_NAME = new ThreadLocal<>();

    /**
     * 线程内缓存：名称 → ID
     */
    private static final ThreadLocal<Map<String, Long>> TL_NAME_TO_ID = new ThreadLocal<>();

    /**
     * 初始化当前线程的部门缓存（一次导出/导入只执行1次）
     */
    private void initThreadCache() {
        Map<Long, String> idMap = TL_ID_TO_NAME.get();
        if (CollUtil.isNotEmpty(idMap)) {
            // 当前线程已加载，直接返回
            return;
        }

        Map<String, Tree<Long>> deptPathToTreeMap = TreeBuildUtils.buildTreeNodeMap(
            SpringUtils.getBean(ISysDeptService.class).selectDeptTreeList(new SysDeptBo()),
            "/",
            Tree::getName
        );

        // 构建互转映射
        Map<Long, String> idToName = new HashMap<>();
        Map<String, Long> nameToId = new HashMap<>();
        deptPathToTreeMap.forEach((name, treeNode) -> {
            Long deptId = treeNode.getId();
            idToName.put(deptId, name);
            nameToId.put(name, deptId);
        });

        // 设置到当前线程
        TL_ID_TO_NAME.set(idToName);
        TL_NAME_TO_ID.set(nameToId);
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

        // 初始化线程缓存（只执行一次）
        initThreadCache();

        // 从线程缓存中直接获取，不查库
        return TL_NAME_TO_ID.get().get(deptName);
    }

    /**
     * 【导出】部门 ID → 转为 Excel 显示的完整路径名称
     */
    @Override
    public WriteCellData<?> convertToExcelData(Long value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }

        // 初始化线程缓存（只执行一次）
        initThreadCache();

        // 从线程缓存中直接获取，不查库
        String deptName = TL_ID_TO_NAME.get().getOrDefault(value, "");
        return new WriteCellData<>(deptName);
    }

}
