package org.dromara.common.translation.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

/**
 * 翻译集合包装器
 *
 * @param <E> 元素类型
 *
 * @author 秋辞未寒
 */
@Getter
@TranslationCollectionBinding
public class TranslationCollectionWrapper<E> implements Collection<E> {

    private final Collection<E> collection;
    private final Class<E> elementType;

    /**
     * 不建议直接使用构造函数去构建包装器
     */
    private TranslationCollectionWrapper(Collection<E> collection, Class<E> elementType) {
        this.collection = collection;
        this.elementType = elementType;
    }

    /**
     * 包装集合成翻译集合
     * @param collection 待包装的集合
     * @return 包装后的集合类型
     * @param <E> 元素类型
     */
    public static <E> TranslationCollectionWrapper<E> form(Collection<E> collection) {
        return form(collection,null);
    }

    /**
     * 包装集合成翻译集合 （推荐使用该函数，可以减少不必要的迭代器取值~）
     * @param collection 待包装的集合
     * @param elementType 元素Class实例
     * @return 包装后的集合类型
     * @param <E> 元素类型
     */
    public static <E> TranslationCollectionWrapper<E> form(Collection<E> collection, Class<E> elementType) {
        if (Objects.isNull(collection)) {
            collection = Collections.emptyList();
        }
        if (Objects.isNull(elementType)) {
            elementType = ClassUtil.getClass(CollUtil.getFirst(collection));
        }
        return new TranslationCollectionWrapper<>(collection,elementType);
    }


    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return collection.iterator();
    }

    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return collection.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return collection.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return collection.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return collection.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return collection.retainAll(c);
    }

    @Override
    public void clear() {
        collection.clear();
    }
}
