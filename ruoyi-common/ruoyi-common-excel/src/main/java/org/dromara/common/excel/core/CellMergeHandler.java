package org.dromara.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.poi.ss.util.CellRangeAddress;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.excel.annotation.CellMerge;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 单元格合并处理器
 *
 * @author Lion Li
 */
public class CellMergeHandler {

    /**
     * 是否包含标题行。
     */
    private final boolean hasTitle;
    /**
     * 当前行索引。
     */
    private int rowIndex;

    /**
     * 创建单元格合并处理器。
     *
     * @param hasTitle 是否存在标题行
     */
    private CellMergeHandler(final boolean hasTitle) {
        this.hasTitle = hasTitle;
        // 行合并开始下标
        this.rowIndex = hasTitle ? 1 : 0;
    }

    /**
     * 创建单元格合并处理器。
     *
     * @param hasTitle 是否存在标题行
     * @param rowIndex 起始行索引
     */
    private CellMergeHandler(final boolean hasTitle, final int rowIndex) {
        this.hasTitle = hasTitle;
        this.rowIndex = hasTitle ? rowIndex : 0;
    }

    /**
     * 计算需要合并的单元格区域。
     *
     * @param rows 数据行
     * @return 单元格合并区域列表
     */
    @SneakyThrows
    public List<CellRangeAddress> handle(List<?> rows) {
        // 如果入参为空集合则返回空集
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        // 获取有合并注解的字段
        Map<Field, FieldColumnIndex> mergeFields = getFieldColumnIndexMap(rows.getFirst().getClass());
        // 如果没有需要合并的字段则返回空集
        if (CollUtil.isEmpty(mergeFields)) {
            return Collections.emptyList();
        }

        // 结果集
        List<CellRangeAddress> result = new ArrayList<>();

        // 生成两两合并单元格
        Map<Field, RepeatCell> rowRepeatCellMap = new HashMap<>();
        for (Map.Entry<Field, FieldColumnIndex> item : mergeFields.entrySet()) {
            Field field = item.getKey();
            FieldColumnIndex itemValue = item.getValue();
            int colNum = itemValue.colIndex();
            CellMerge cellMerge = itemValue.cellMerge();

            for (int i = 0; i < rows.size(); i++) {
                // 当前行数据
                Object currentRowObj = rows.get(i);
                // 当前行数据字段值
                Object currentRowObjFieldVal = ReflectUtils.invokeGetter(currentRowObj, field.getName());

                // 空值视为合并中断，需要先收口上一段合并区间
                if (isBlankCell(currentRowObjFieldVal)) {
                    appendMergeResult(result, rowRepeatCellMap.get(field), i - 1, colNum);
                    rowRepeatCellMap.remove(field);
                    continue;
                }

                // 单元格合并Map是否存在数据，如果不存在则添加当前行的字段值
                if (!rowRepeatCellMap.containsKey(field)) {
                    rowRepeatCellMap.put(field, RepeatCell.of(currentRowObjFieldVal, i));
                    continue;
                }

                // 获取 单元格合并Map 中字段值
                RepeatCell repeatCell = rowRepeatCellMap.get(field);
                Object cellValue = repeatCell.value();

                // 检查是否满足合并条件
                // currentRowObj 当前行数据
                // rows.get(i - 1) 上一行数据 注：由于 if (!rowRepeatCellMap.containsKey(field)) 条件的存在，所以该 i 必不可能小于1
                // cellMerge 当前行字段合并注解
                boolean merge = isMerge(currentRowObj, rows.get(i - 1), cellMerge);

                // 如果当前行字段值和缓存中的字段值不相等，或不满足合并条件，则替换
                if (!currentRowObjFieldVal.equals(cellValue) || !merge) {
                    appendMergeResult(result, repeatCell, i - 1, colNum);
                    rowRepeatCellMap.put(field, RepeatCell.of(currentRowObjFieldVal, i));
                }
            }
            appendMergeResult(result, rowRepeatCellMap.get(field), rows.size() - 1, colNum);
            rowRepeatCellMap.remove(field);
        }
        return result;
    }

    /**
     * 获取带有合并注解的字段列索引和合并注解信息Map集
     */
    private Map<Field, FieldColumnIndex> getFieldColumnIndexMap(Class<?> clazz) {
        boolean annotationPresent = clazz.isAnnotationPresent(ExcelIgnoreUnannotated.class);
        Field[] fields = ReflectUtils.getFields(clazz, field -> {
            if ("serialVersionUID".equals(field.getName())) {
                return false;
            }
            if (field.isAnnotationPresent(ExcelIgnore.class)) {
                return false;
            }
            return !annotationPresent || field.isAnnotationPresent(ExcelProperty.class);
        });

        // 有注解的字段
        Map<Field, FieldColumnIndex> mergeFields = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (!field.isAnnotationPresent(CellMerge.class)) {
                continue;
            }
            CellMerge cm = field.getAnnotation(CellMerge.class);
            ExcelProperty property = field.getAnnotation(ExcelProperty.class);
            int index = cm.index();
            if (index == -1) {
                index = property != null && property.index() != -1 ? property.index() : i;
            }
            mergeFields.put(field, FieldColumnIndex.of(index, cm));

            if (hasTitle && property != null && property.value().length > 0) {
                rowIndex = Math.max(rowIndex, property.value().length);
            }
        }
        return mergeFields;
    }

    /**
     * 判断当前行是否满足依赖字段合并条件。
     *
     * @param currentRow 当前行数据
     * @param preRow     上一行数据
     * @param cellMerge  合并配置
     * @return 是否允许合并
     */
    private boolean isMerge(Object currentRow, Object preRow, CellMerge cellMerge) {
        final String[] mergeBy = cellMerge.mergeBy();
        if (StrUtil.isAllNotBlank(mergeBy)) {
            // 比对当前行和上一行的各个属性值一一比对 如果全为真 则为真
            for (String fieldName : mergeBy) {
                final Object valCurrent = ReflectUtil.getFieldValue(currentRow, fieldName);
                final Object valPre = ReflectUtil.getFieldValue(preRow, fieldName);
                if (!Objects.equals(valPre, valCurrent)) {
                    // 依赖字段如有任一不等值,则标记为不可合并
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断单元格值是否为空。
     *
     * @param value 单元格值
     * @return 是否为空
     */
    private boolean isBlankCell(Object value) {
        return value == null || StrUtil.isBlankIfStr(value);
    }

    /**
     * 追加有效的合并区域。
     *
     * @param result     合并区域结果集
     * @param repeatCell 连续重复单元格信息
     * @param endIndex   结束行索引
     * @param colNum     列索引
     */
    private void appendMergeResult(List<CellRangeAddress> result, RepeatCell repeatCell, int endIndex, int colNum) {
        if (repeatCell == null || endIndex <= repeatCell.current()) {
            return;
        }
        result.add(new CellRangeAddress(repeatCell.current() + rowIndex, endIndex + rowIndex, colNum, colNum));
    }

    /**
     * 单元格合并
     */
    record RepeatCell(Object value, int current) {
        /**
         * 创建连续重复单元格信息。
         *
         * @param value   单元格值
         * @param current 当前行索引
         * @return 连续重复单元格信息
         */
        static RepeatCell of(Object value, int current) {
            return new RepeatCell(value, current);
        }
    }

    /**
     * 字段列索引和合并注解信息
     */
    record FieldColumnIndex(int colIndex, CellMerge cellMerge) {
        /**
         * 创建字段列索引和合并注解信息。
         *
         * @param colIndex  列索引
         * @param cellMerge 合并注解
         * @return 字段列索引和合并注解信息
         */
        static FieldColumnIndex of(int colIndex, CellMerge cellMerge) {
            return new FieldColumnIndex(colIndex, cellMerge);
        }
    }

    /**
     * 创建一个单元格合并处理器实例
     *
     * @param hasTitle 是否合并标题
     * @param rowIndex 行索引
     * @return 单元格合并处理器
     */
    public static CellMergeHandler of(final boolean hasTitle, final int rowIndex) {
        return new CellMergeHandler(hasTitle, rowIndex);
    }

    /**
     * 创建一个单元格合并处理器实例
     *
     * @param hasTitle 是否合并标题
     * @return 单元格合并处理器
     */
    public static CellMergeHandler of(final boolean hasTitle) {
        return new CellMergeHandler(hasTitle);
    }

    /**
     * 创建一个单元格合并处理器实例（默认不合并标题）
     *
     * @return 单元格合并处理器
     */
    public static CellMergeHandler of() {
        return new CellMergeHandler(false);
    }

}
