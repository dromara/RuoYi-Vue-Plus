package com.ruoyi.common.translation.core.impl;

import com.ruoyi.common.core.service.OssService;
import com.ruoyi.common.translation.annotation.TranslationType;
import com.ruoyi.common.translation.constant.TransConstant;
import com.ruoyi.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OSS翻译实现
 *
 * @author Lion Li
 */
@Component
@AllArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
public class OssUrlTranslationImpl implements TranslationInterface {

    private final OssService ossService;

    public String translation(Object key, String other) {
        if (key instanceof String ids) {
            return ossService.selectUrlByIds(ids);
        }
        return null;
    }
}
