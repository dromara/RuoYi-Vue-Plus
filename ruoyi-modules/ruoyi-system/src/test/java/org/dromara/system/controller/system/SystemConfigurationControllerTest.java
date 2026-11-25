package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.system.domain.bo.*;
import org.dromara.system.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemConfigurationControllerTest {

    @Test
    void duplicateRoleNameBlocksCreationBeforeAnyLinks() {
        var roles = mock(ISysRoleService.class);
        var role = new SysRoleBo();
        role.setRoleName("采购员");
        when(roles.checkRoleNameUnique(role)).thenReturn(false);
        var result = new SysRoleController(roles, mock(), mock()).add(role);
        assertTrue(R.isError(result));
        assertTrue(result.getMsg().contains("角色名称已存在"));
        verify(roles, never()).insertRole(any());
    }

    @Test
    void updateByKeyUsesRequestedKeyEvenWhenPayloadContainsAnId() {
        var service = mock(ISysConfigService.class);
        var request = new SysConfigBo();
        request.setConfigId(91L);
        request.setConfigKey("site.title");
        request.setConfigValue("Portal");
        var response = new SysConfigController(service).updateByKey(request);
        assertTrue(R.isSuccess(response));
        verify(service).updateConfig(argThat(config -> config.getConfigId() == null
            && "site.title".equals(config.getConfigKey()) && "Portal".equals(config.getConfigValue())));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void updateByKeyRequiresANonblankKey(String key) {
        var service = mock(ISysConfigService.class);
        var request = new SysConfigBo();
        request.setConfigId(91L);
        request.setConfigKey(key);
        request.setConfigValue("Portal");
        assertTrue(R.isError(new SysConfigController(service).updateByKey(request)));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsDuplicateConfigKeyBeforeInsert() {
        var service = mock(ISysConfigService.class);
        var request = new SysConfigBo();
        request.setConfigKey("site.title");
        when(service.checkConfigKeyUnique(request)).thenReturn(false);

        var response = new SysConfigController(service).add(request);

        assertTrue(R.isError(response));
        verify(service).checkConfigKeyUnique(request);
        verify(service, never()).insertConfig(any());
    }

    @Test
    void rejectsDuplicateClientKeyBeforeInsert() {
        var service = mock(ISysClientService.class);
        var request = new SysClientBo();
        request.setClientKey("desktop");
        when(service.checkClickKeyUnique(request)).thenReturn(false);

        var response = new SysClientController(service).add(request);

        assertTrue(R.isError(response));
        verify(service).checkClickKeyUnique(request);
        verify(service, never()).insertByBo(any());
    }

    @Test
    void rejectsDuplicateDictTypeBeforeInsert() {
        var service = mock(ISysDictTypeService.class);
        var request = new SysDictTypeBo();
        request.setDictType("sys_status");
        when(service.checkDictTypeUnique(request)).thenReturn(false);

        var response = new SysDictTypeController(service).add(request);

        assertTrue(R.isError(response));
        verify(service).checkDictTypeUnique(request);
        verify(service, never()).insertDictType(any());
    }

    @Test
    void rejectsDuplicateRoleKeyBeforeInsert() {
        var service = mock(ISysRoleService.class);
        var request = new SysRoleBo();
        request.setRoleKey("editor");
        when(service.checkRoleNameUnique(request)).thenReturn(true);
        when(service.checkRoleKeyUnique(request)).thenReturn(false);

        var response = new SysRoleController(service, mock(), mock()).add(request);

        assertTrue(R.isError(response));
        verify(service).checkRoleKeyUnique(request);
        verify(service, never()).insertRole(any());
    }

    @Test
    void rejectsDuplicatePostCodeBeforeInsert() {
        var service = mock(ISysPostService.class);
        var request = new SysPostBo();
        request.setPostCode("finance");
        when(service.checkPostNameUnique(request)).thenReturn(true);
        when(service.checkPostCodeUnique(request)).thenReturn(false);

        var response = new SysPostController(service, mock()).add(request);

        assertTrue(R.isError(response));
        verify(service).checkPostCodeUnique(request);
        verify(service, never()).insertPost(any());
    }

    @Test
    void unknownDictionaryTypeReturnsAnEmptyList() {
        var types = mock(ISysDictTypeService.class);
        when(types.selectDictDataByType("missing")).thenReturn(null);

        var response = new SysDictDataController(mock(), types).dictType("missing");

        assertTrue(R.isSuccess(response));
        assertEquals(List.of(), response.getData());
        verify(types).selectDictDataByType("missing");
    }
}
