package com.wudgaby.stars.support;

import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 标签 CSV 解析工具
 */
public final class StarsTagCsvParser {

    private StarsTagCsvParser() {
    }

    public static List<String> parseNames(String csv) {
        if (StringUtils.isBlank(csv)) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    public static List<Long> parseIds(String csv) {
        if (StringUtils.isBlank(csv)) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(Long::valueOf)
            .toList();
    }

}
