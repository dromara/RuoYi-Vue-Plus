package org.dromara.system.listener;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.extra.spring.SpringUtil;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.TreeBuildUtils;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.service.ISysDeptService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeptExcelConverterTest {

    @Test
    void shouldRoundTripDepartmentPathsThroughUserWorkbooks() throws Exception {
        var departments = mock(ISysDeptService.class);
        var root = new Tree<Long>(TreeBuildUtils.DEFAULT_CONFIG).setId(1L).setName("总部");
        var child = new Tree<Long>(TreeBuildUtils.DEFAULT_CONFIG).setId(42L).setName("销售部");
        root.setChildren(List.of(child));
        when(departments.selectDeptTreeList(any())).thenReturn(List.of(root));
        var dictionaries = mock(DictService.class);
        var user = new SysUserExportVo();
        user.setDeptId(42L);

        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(ISysDeptService.class)).thenReturn(departments);
            spring.when(() -> SpringUtil.getBean(DictService.class)).thenReturn(dictionaries);
            for (String name : List.of("销售部", "市场部")) {
                child.setName(name);
                var output = new ByteArrayOutputStream();
                ExcelBuilder.of(List.of(user, user), SysUserExportVo.class).toStream(output);
                byte[] bytes = output.toByteArray();
                try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                    var sheet = workbook.getSheetAt(0);
                    assertEquals("部门名称", sheet.getRow(0).getCell(2).getStringCellValue());
                    assertEquals("总部/" + name, sheet.getRow(1).getCell(2).getStringCellValue());
                }
                var imported = ExcelBuilder.read(new ByteArrayInputStream(bytes), SysUserExportVo.class).doReadSync();
                assertEquals(List.of(42L, 42L), imported.stream().map(SysUserExportVo::getDeptId).toList());
            }
            verify(departments, times(4)).selectDeptTreeList(any());
        }
    }

    @Test
    void shouldUseRenamedDepartmentInNextWorkbook() {
        var departments = mock(ISysDeptService.class);
        var original = new Tree<Long>(TreeBuildUtils.DEFAULT_CONFIG).setId(42L).setName("销售部");
        var renamed = new Tree<Long>(TreeBuildUtils.DEFAULT_CONFIG).setId(42L).setName("市场部");
        when(departments.selectDeptTreeList(any())).thenReturn(List.of(original)).thenReturn(List.of(renamed));

        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(ISysDeptService.class)).thenReturn(departments);
            var converter = new DeptExcelConverter();
            assertEquals("销售部", converter.convertToExcelData(42L, null, new GlobalConfiguration()).getStringValue());

            var nextWorkbook = new GlobalConfiguration();
            assertAll(
                () -> assertEquals("市场部", converter.convertToExcelData(42L, null, nextWorkbook).getStringValue()),
                () -> assertEquals(42L, converter.convertToJavaData(new ReadCellData<>("市场部"), null, nextWorkbook)),
                () -> assertThrows(ServiceException.class,
                    () -> converter.convertToJavaData(new ReadCellData<>("销售部"), null, nextWorkbook))
            );
            verify(departments, times(2)).selectDeptTreeList(any());
        }
    }
}
