package org.dromara.common.translation.collection;

import java.util.Collection;

/**
 * 翻译集合处理器
 * @param <T> 输入类型
 * @param <R> 目标类型
 *
 * @author 秋辞未寒
 */
public interface TranslationCollectionProcessor<T,R> {


    Collection<R> process(Collection<T> value,String other) throws Exception;

}
