package org.dromara.workflow.service.impl;

import org.dromara.common.core.service.DictService;
import org.dromara.common.core.domain.dto.DictTypeDTO;
import org.dromara.common.core.domain.dto.DictDataDTO;
import org.dromara.warm.flow.ui.vo.NodeExt;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeExtRulesTest {
    @Test
    void emptyConfigurationHasNoOverrides() {
        var ext = new FlwNodeExtServiceImpl(null).parseNodeExt(null, Map.of());
        assertNull(ext.getButtonPermissions());
        assertNull(ext.getVariables());
    }

    @Test
    void designerOptionsPreserveEnumAndDictionaryValuesAndLabels() {
        var dictionaries = mock(DictService.class);
        var service = new FlwNodeExtServiceImpl(dictionaries);
        var button = service.getNodeExt().stream().filter(tab -> "wf_button_tab".equals(tab.getCode()))
            .findFirst().orElseThrow().getChilds().getFirst();
        var back = button.getDict().stream().filter(item -> "back".equals(item.getValue())).findFirst().orElseThrow();
        assertEquals("是否显示退回", back.getLabel());
        var type = new DictTypeDTO();
        type.setDictName("优先级");
        var row = new DictDataDTO();
        row.setDictLabel("紧急");
        row.setDictValue("urgent");
        when(dictionaries.getDictType("priority")).thenReturn(type);
        when(dictionaries.getDictData("priority")).thenReturn(List.of(row));
        NodeExt.ChildNode child = ReflectionTestUtils.invokeMethod(service, "buildChildNode", "priority");
        assertEquals("priority", child.getCode());
        assertEquals("优先级", child.getLabel());
        assertEquals(List.of("urgent"), child.getDict().stream().map(NodeExt.DictItem::getValue).toList());
        assertEquals(List.of("紧急"), child.getDict().stream().map(NodeExt.DictItem::getLabel).toList());
    }
    @Test
    void returnsCheckedButtonsAndVariablesFromSameNode() {
        var service = new FlwNodeExtServiceImpl(null);
        String config = """
            [{"code":"ButtonPermissionEnum","value":"back"},
             {"code":"VariablesEnum","value":"days=3,missing,dept=7"}]
            """;
        try (var spring = mockStatic(cn.hutool.extra.spring.SpringUtil.class)) {
            spring.when(() -> cn.hutool.extra.spring.SpringUtil.getBean(tools.jackson.databind.json.JsonMapper.class))
                .thenReturn(tools.jackson.databind.json.JsonMapper.builder().build());
            var ext = service.parseNodeExt(config, Map.of());
            assertEquals(Map.of("days", "3", "dept", "7"), ext.getVariables());
            assertEquals(List.of("back"), ext.getButtonPermissions().stream().filter(button -> button.getShow())
                .map(button -> button.getCode()).toList());
        }
    }
}
