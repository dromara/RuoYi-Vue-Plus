package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.bo.*;
import org.dromara.system.domain.vo.*;
import org.dromara.system.service.*;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationMaintenanceTest {

    @Test
    void rejectsDuplicateSiblingAndKeepsDepartmentSelectionScoped() {
        var candidate = new SysDeptBo();
        candidate.setParentId(42L);
        candidate.setDeptName("销售");
        when(depts.checkDeptNameUnique(candidate)).thenReturn(false);
        assertTrue(R.isError(departments.add(candidate)));
        verify(depts, never()).insertDept(any());
        var rows = List.of(department(42L, "0"));
        when(depts.selectDeptByIds(List.of(42L, 44L))).thenReturn(rows);
        assertEquals(rows, departments.optionselect(new Long[]{42L,44L}).getData());
    }

    private final ISysDeptService depts = mock();
    private final ISysPostService posts = mock();
    private final SysDeptController departments = new SysDeptController(depts, posts);
    private final SysPostController positions = new SysPostController(posts, depts);

    @ParameterizedTest
    @ValueSource(strings = {"children", "users", "posts", "empty"})
    void onlyDeletesDepartmentsWithoutDependants(String contents) {
        when(depts.hasChildByDeptId(27L)).thenReturn(contents.equals("children"));
        when(depts.checkDeptExistUser(27L)).thenReturn(contents.equals("users"));
        when(posts.countPostByDeptId(27L)).thenReturn(contents.equals("posts") ? 1L : 0L);
        when(depts.deleteDeptById(27L)).thenReturn(1);
        var response = departments.remove(27L);
        boolean allowed = contents.equals("empty");
        assertEquals(allowed, R.isSuccess(response));
        verify(depts, times(allowed ? 1 : 0)).deleteDeptById(27L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"duplicate", "self", "children", "users", "empty"})
    void validatesDisabledDepartmentBeforeUpdate(String scenario) {
        var dept = new SysDeptBo();
        dept.setDeptId(27L);
        dept.setParentId(scenario.equals("self") ? 27L : 1L);
        dept.setStatus(SystemConstants.DISABLE);
        when(depts.checkDeptNameUnique(dept)).thenReturn(!scenario.equals("duplicate"));
        when(depts.selectNormalChildrenDeptById(27L)).thenReturn(scenario.equals("children") ? 1L : 0L);
        when(depts.checkDeptExistUser(27L)).thenReturn(scenario.equals("users"));
        when(depts.updateDept(dept)).thenReturn(1);
        boolean allowed = scenario.equals("empty");
        assertEquals(allowed, R.isSuccess(departments.edit(dept)));
        verify(depts, times(allowed ? 1 : 0)).updateDept(dept);
    }

    @Test
    void excludesOnlyTheRequestedBranchFromParentOptions() {
        var root = department(2L, "0");
        var descendant = department(9L, "0,2,7");
        var sibling = department(12L, "0,22");
        when(depts.selectDeptList(any())).thenReturn(new ArrayList<>(List.of(root, descendant, sibling)));
        assertEquals(List.of(sibling), departments.excludeChild(2L).getData());
    }

    private SysDeptVo department(long id, String ancestors) {
        var dept = new SysDeptVo();
        dept.setDeptId(id);
        dept.setAncestors(ancestors);
        return dept;
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "code", "assigned", "unassigned"})
    void rejectsConflictingOrAssignedPostBeforeDisabling(String scenario) {
        var post = new SysPostBo();
        post.setPostId(14L);
        post.setStatus(SystemConstants.DISABLE);
        when(posts.checkPostNameUnique(post)).thenReturn(!scenario.equals("name"));
        when(posts.checkPostCodeUnique(post)).thenReturn(!scenario.equals("code"));
        when(posts.countUserPostById(14L)).thenReturn(scenario.equals("assigned") ? 1L : 0L);
        when(posts.updatePost(post)).thenReturn(1);
        boolean allowed = scenario.equals("unassigned");
        assertEquals(allowed, R.isSuccess(positions.edit(post)));
        verify(posts, times(allowed ? 1 : 0)).updatePost(post);
    }

    @Test
    void departmentFilterTakesPrecedenceOverPostIds() {
        var result = List.of(new SysPostVo());
        when(posts.selectPostList(argThat(post -> Long.valueOf(27).equals(post.getDeptId())))).thenReturn(result);
        assertEquals(result, positions.optionselect(new Long[]{14L}, 27L).getData());
        verify(posts, never()).selectPostByIds(any());
    }

    @Test
    void postIdFilterSelectsRequestedIds() {
        var result = List.of(new SysPostVo());
        when(posts.selectPostByIds(List.of(14L, 15L))).thenReturn(result);
        assertEquals(result, positions.optionselect(new Long[]{14L, 15L}, null).getData());
    }

    @Test
    void emptyPostSelectionReturnsNoOptions() {
        assertEquals(List.of(), positions.optionselect(null, null).getData());
        verifyNoInteractions(posts);
    }
}
