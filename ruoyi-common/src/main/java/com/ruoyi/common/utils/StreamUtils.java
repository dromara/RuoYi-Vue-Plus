package com.ruoyi.common.utils;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StreamUtils extends CollStreamUtil {

    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream().filter(function).collect(Collectors.toList());
    }

    public static <E> String join(Collection<E> collection,  Function<E, String> function) {
        return join(collection, function, ",");
    }

    public static <E> String join(Collection<E> collection,  Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return StringUtils.EMPTY;
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        return collection.stream().sorted(comparing).collect(Collectors.toList());
    }
}
