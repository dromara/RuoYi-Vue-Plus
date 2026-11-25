package org.dromara.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.system.api.DeptService;

import java.util.Map;
import java.util.Set;

/**
 * 部门翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.DEPT_ID_TO_NAME)
public class DeptNameTranslationImpl implements TranslationInterface<String> {

    private final DeptService deptService;

    /**
     * 将部门 ID 或 ID 集合翻译为部门名称。
     *
     * @param key   部门 ID 或逗号分隔的 ID 字符串
     * @param other 额外参数
     * @return 部门名称
     */
    @Override
    public String translation(Object key, String other) {
        if (key instanceof String ids) {
            return deptService.selectDeptNameByIds(ids);
        } else if (key instanceof Long id) {
            return deptService.selectDeptNameByIds(id.toString());
        }
        return null;
    }

    /**
     * 批量将部门 ID 翻译为部门名称。
     *
     * @param keys  部门 ID 集合
     * @param other 额外参数
     * @return 部门 ID 与部门名称映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        return translateLongOrStringKeys(keys, deptService::selectDeptNamesByIds);
    }
}
