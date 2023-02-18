package com.ruoyi.common.translation;

/**
 * 翻译接口 (实现类需标注 {@link com.ruoyi.common.annotation.TranslationType} 注解标明翻译类型)
 *
 * @author Lion Li
 */
public interface TranslationInterface<T> {

    /**
     * 翻译
     *
     * @param key 需要被翻译的键(不为空)
     * @return 返回键对应的值
     */
    T translation(Object key, String other);
}
