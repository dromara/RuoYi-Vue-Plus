/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.ui.utils;

import cn.hutool.core.util.StrUtil;

import org.dromara.warm.flow.dto.Tree;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TreeUtil {
    private TreeUtil() {
    }

    /**
     * 构建所需要树结构
     *
     * @param trees 部门列表
     * @return 树结构列表
     */
    public static List<Tree> buildTree(List<Tree> trees) {
        List<Tree> returnList = new ArrayList<>();
        List<String> tempList = trees.stream().map(Tree::getId).collect(Collectors.toList());
        for (Tree dept : trees) {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId())) {
                recursionFn(trees, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty()) {
            returnList = trees;
        }
        return returnList;
    }

    /**
     * 递归列表
     */
    private static void recursionFn(List<Tree> list, Tree t) {
        // 得到子节点列表
        List<Tree> childList = getChildList(list, t);
        t.setChildren(childList);
        for (Tree tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 判断是否有子节点
     */
    private static boolean hasChild(List<Tree> list, Tree t) {
        return !getChildList(list, t).isEmpty();
    }

    /**
     * 得到子节点列表
     */
    private static List<Tree> getChildList(List<Tree> list, Tree t) {
        List<Tree> tlist = new ArrayList<>();
        for (Tree n : list) {
            if (StrUtil.isNotEmpty(n.getParentId()) && n.getParentId().equals(t.getId())) {
                tlist.add(n);
            }
        }
        return tlist;
    }
}
