package com.ruoyi.common.utils;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.lang.tree.parser.NodeParser;

import java.util.List;

/**
 * 扩展 hutool TreeUtil 封装系统树构建
 *
 * @author Lion Li
 */
public class TreeBuildUtils extends TreeUtil {

    /**
     * 根据前端定制差异化字段
     */
    public static final TreeNodeConfig DEFAULT_CONFIG = TreeNodeConfig.DEFAULT_CONFIG.setNameKey("label");

    /**
     * 默认树父节点id
     */
    public static final Long DEFAULT_PARENT_ID = 0L;

    public static <T> List<Tree<Long>> build(List<T> list, NodeParser<T, Long> nodeParser) {
        return TreeUtil.build(list, DEFAULT_PARENT_ID, DEFAULT_CONFIG, nodeParser);
    }

}
