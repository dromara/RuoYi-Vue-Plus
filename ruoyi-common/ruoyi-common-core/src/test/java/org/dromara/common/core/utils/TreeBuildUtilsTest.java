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

    @Test
    @DisplayName("不同父级的可见根节点按业务权重统一排序")
    void shouldOrderRootsByWeightAcrossParentGroups() {
        record Department(long id, long parentId, int orderNum) {}
        var departments = List.of(new Department(1, 0, 30), new Department(2, 99, 20),
            new Department(3, 0, 10), new Department(4, 2, 1));

        var roots = TreeBuildUtils.buildMultiRoot(departments, Department::id, Department::parentId,
            (dept, node) -> node.setId(dept.id()).setParentId(dept.parentId()).setWeight(dept.orderNum()));

        assertEquals(List.of(3L, 2L, 1L), roots.stream().map(Tree::getId).toList());
        assertEquals(List.of(4L), roots.get(1).getChildren().stream().map(Tree::getId).toList());
    }

    @Test
    @DisplayName("多根树同时保留无父节点和父节点未返回的分支")
    void shouldKeepNullParentAlongsideMissingParent() {
        List<TestNode> nodes = List.of(
            new TestNode(2L, 1L, "研发部"),
            new TestNode(1L, null, "总部"),
            new TestNode(3L, 99L, "外地分部"));

        List<Tree<Long>> roots = TreeBuildUtils.buildMultiRoot(nodes, TestNode::id, TestNode::parentId,
            (node, tree) -> tree.setId(node.id()).setParentId(node.parentId()).setName(node.name()));

        assertEquals(Set.of("总部", "总部/研发部", "外地分部"),
            TreeBuildUtils.buildTreeNodeMap(roots, "/", Tree::getName).keySet());
    }

    @Test
    @DisplayName("子菜单先出现时仍保留顶级菜单及完整层级")
    void shouldBuildCompleteTreeWhenChildComesFirst() {
        Tree<Long> root = tree(1L, "系统管理").setParentId(0L);
        Tree<Long> child = tree(2L, "用户管理").setParentId(1L);
        Tree<Long> leaf = tree(3L, "用户详情").setParentId(2L);
        List<Tree<Long>> result = TreeBuildUtils.build(List.of(child, leaf, root),
            (source, node) -> node.setId(source.getId()).setParentId(source.getParentId()).setName(source.getName()));

        assertEquals(List.of(1L), result.stream().map(Tree::getId).toList());
        assertEquals(List.of("系统管理", "系统管理/用户管理", "系统管理/用户管理/用户详情"),
            List.copyOf(TreeBuildUtils.buildTreeNodeMap(result, "/", Tree::getName).keySet()));
    }

    @Test
    @DisplayName("构建前端树时保留 Hutool 节点的标准名称字段")
    void shouldKeepHutoolNamesIndependentOfFrontendLabels() {
        Tree<Long> existing = new Tree<>();
        existing.put("name", "原节点");
        var result = TreeBuildUtils.build(List.of(new TestNode(1L, 0L, "前端节点")), 0L,
            (node, tree) -> tree.setId(node.id()).setParentId(node.parentId()).setName(node.name()));

        assertEquals("前端节点", result.getFirst().get("label"));
        assertEquals("原节点", existing.getName());
        assertEquals("新节点", tree(2L, "新节点").get("name"));
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
        assertEquals("root-a", firstRoot.get("label"));
        assertEquals("child-a", firstRoot.getChildren().getFirst().get("label"));
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
