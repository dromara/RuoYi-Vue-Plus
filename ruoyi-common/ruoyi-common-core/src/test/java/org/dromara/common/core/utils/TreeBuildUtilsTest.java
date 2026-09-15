package org.dromara.common.core.utils;

import cn.hutool.core.lang.tree.Tree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TreeBuildUtils 单元测试")
class TreeBuildUtilsTest {

    static {
        // TreeBuildUtils 会将 Hutool 默认名称键调整为前端使用的 label。
        TreeBuildUtils.DEFAULT_CONFIG.getNameKey();
    }

    /**
     * 验证递归遍历只返回没有子节点的叶子节点。
     */
    @Test
    @DisplayName("获取树中所有叶子节点")
    void getLeafNodesShouldReturnOnlyLeaves() {
        Tree<Long> root = tree(1L, "root");
        Tree<Long> leaf = tree(2L, "leaf");
        Tree<Long> branch = tree(3L, "branch");
        Tree<Long> nestedLeaf = tree(4L, "nested");
        branch.setChildren(List.of(nestedLeaf));
        root.setChildren(List.of(leaf, branch));

        List<Tree<Long>> result = TreeBuildUtils.getLeafNodes(List.of(root));

        assertEquals(List.of(2L, 4L), result.stream().map(Tree::getId).toList());
        assertTrue(TreeBuildUtils.<Long>getLeafNodes(null).isEmpty());
    }

    /**
     * 验证树节点按照深度优先顺序生成完整路径键。
     */
    @Test
    @DisplayName("按深度优先顺序构建节点路径映射")
    void buildTreeNodeMapShouldCreateFullPaths() {
        Tree<Long> root = tree(1L, "root");
        Tree<Long> child = tree(2L, "child");
        Tree<Long> leaf = tree(3L, "leaf");
        child.setChildren(List.of(leaf));
        root.setChildren(List.of(child));

        Map<String, Tree<Long>> result = TreeBuildUtils.buildTreeNodeMap(List.of(root), "/", Tree::getName);

        assertEquals(List.of("root", "root/child", "root/child/leaf"), result.keySet().stream().toList());
        assertEquals(3L, result.get("root/child/leaf").getId());
    }

    /**
     * 验证不同父级来源的顶级节点可以合并为多根树。
     */
    @Test
    @DisplayName("构建包含多个顶级节点的树")
    void buildMultiRootShouldKeepAllRoots() {
        List<TestNode> nodes = List.of(
            new TestNode(1L, 0L, "root-a"),
            new TestNode(2L, 1L, "child-a"),
            new TestNode(10L, 9L, "root-b")
        );

        List<Tree<Long>> result = TreeBuildUtils.buildMultiRoot(nodes, TestNode::id, TestNode::parentId,
            (node, treeNode) -> treeNode.setId(node.id()).setParentId(node.parentId()).setName(node.name()));

        Set<Long> rootIds = result.stream().map(Tree::getId).collect(Collectors.toSet());
        assertEquals(Set.of(1L, 10L), rootIds);
        Tree<Long> firstRoot = result.stream().filter(tree -> tree.getId().equals(1L)).findFirst().orElseThrow();
        assertEquals(List.of(2L), firstRoot.getChildren().stream().map(Tree::getId).toList());
    }

    /**
     * 创建测试使用的最小树节点。
     *
     * @param id   节点 ID
     * @param name 节点名称
     * @return 树节点
     */
    private static Tree<Long> tree(Long id, String name) {
        Tree<Long> tree = new Tree<>();
        tree.setId(id);
        tree.setName(name);
        return tree;
    }

    private record TestNode(Long id, Long parentId, String name) {
    }

}
