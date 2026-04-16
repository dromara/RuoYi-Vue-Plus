package org.dromara.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.system.api.DeptService;

import java.util.LinkedHashMap;
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
     * @param key 部门 ID 或逗号分隔的 ID 字符串
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

    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> deptIds = collectLongIds(keys);
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> deptNames = deptService.selectDeptNamesByIds(deptIds);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, deptNames));
        }
        return result;
    }

    private String buildValue(Object source, Map<Long, String> deptNames) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, deptNames::get);
        }
        return source == null ? null : deptNames.get(Convert.toLong(source));
    }
}
