package org.dromara.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.dromara.common.core.domain.dto.OssDTO;
import org.dromara.common.core.service.OssService;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OSS翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
public class OssUrlTranslationImpl implements TranslationInterface<String> {

    private final OssService ossService;

    /**
     * 将 OSS ID 或 ID 集合翻译为访问地址。
     *
     * @param key OSS ID 或逗号分隔的 ID 字符串
     * @param other 额外参数
     * @return 访问地址
     */
    @Override
    public String translation(Object key, String other) {
        if (key instanceof String ids) {
            return ossService.selectUrlByIds(ids);
        } else if (key instanceof Long id) {
            return ossService.selectUrlByIds(id.toString());
        }
        return null;
    }

    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> ossIds = collectLongIds(keys);
        if (ossIds.isEmpty()) {
            return Map.of();
        }
        String idText = ossIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<Long, String> ossUrls = new LinkedHashMap<>(StreamUtils.toMap(ossService.selectByIds(idText), OssDTO::getOssId, OssDTO::getUrl));
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, ossUrls));
        }
        return result;
    }

    private String buildValue(Object source, Map<Long, String> ossUrls) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, ossUrls::get);
        }
        return source == null ? null : ossUrls.get(Convert.toLong(source));
    }
}
