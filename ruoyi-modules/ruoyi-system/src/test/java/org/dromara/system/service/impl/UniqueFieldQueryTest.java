package org.dromara.system.service.impl;

import org.dromara.system.domain.*;
import org.dromara.system.domain.bo.*;
import org.dromara.system.domain.vo.*;
import org.dromara.system.mapper.*;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UniqueFieldQueryTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkPostNameUniqueExcludesEditedRecord(boolean edit) {
        var mapper = mock(SysPostMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "Post"), SysPost.class);
        when(mapper.currentModelClass()).thenReturn(SysPost.class);
        var query = new LambdaCrudChainWrapper<SysPost, SysPostVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var bo = new SysPostBo();
        bo.setPostName("采购岗");
        bo.setPostId(edit ? 27L : null);
        bo.setDeptId(12L);
        when(mapper.exists(query)).thenReturn(true);
        assertFalse(new SysPostServiceImpl(mapper, mock(), mock()).checkPostNameUnique(bo));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("post_name ="), sql);
        assertEquals(edit, sql.contains("post_id <>"));
        var values = query.getParamNameValuePairs().values();
        assertTrue(values.contains("采购岗"));
        assertEquals(edit, values.contains(27L));
        assertTrue(sql.contains("dept_id =") && values.contains(12L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkPostCodeUniqueExcludesEditedRecord(boolean edit) {
        var mapper = mock(SysPostMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "Post"), SysPost.class);
        when(mapper.currentModelClass()).thenReturn(SysPost.class);
        var query = new LambdaCrudChainWrapper<SysPost, SysPostVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var bo = new SysPostBo();
        bo.setPostCode("buyer");
        bo.setPostId(edit ? 27L : null);
        when(mapper.exists(query)).thenReturn(true);
        assertFalse(new SysPostServiceImpl(mapper, mock(), mock()).checkPostCodeUnique(bo));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("post_code ="), sql);
        assertEquals(edit, sql.contains("post_id <>"));
        var values = query.getParamNameValuePairs().values();
        assertTrue(values.contains("buyer"));
        assertEquals(edit, values.contains(27L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkClickKeyUniqueExcludesEditedRecord(boolean edit) {
        var mapper = mock(SysClientMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "Client"), SysClient.class);
        when(mapper.currentModelClass()).thenReturn(SysClient.class);
        var query = new LambdaCrudChainWrapper<SysClient, SysClientVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var bo = new SysClientBo();
        bo.setClientKey("desk");
        bo.setId(edit ? 27L : null);
        when(mapper.exists(query)).thenReturn(true);
        assertFalse(new SysClientServiceImpl(mapper).checkClickKeyUnique(bo));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("client_key ="), sql);
        assertEquals(edit, sql.contains("id <>"));
        var values = query.getParamNameValuePairs().values();
        assertTrue(values.contains("desk"));
        assertEquals(edit, values.contains(27L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkDictDataUniqueExcludesEditedRecord(boolean edit) {
        var mapper = mock(SysDictDataMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "DictData"), SysDictData.class);
        when(mapper.currentModelClass()).thenReturn(SysDictData.class);
        var query = new LambdaCrudChainWrapper<SysDictData, SysDictDataVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var bo = new SysDictDataBo();
        bo.setDictValue("open");
        bo.setDictCode(edit ? 27L : null);
        bo.setDictType("status");
        when(mapper.exists(query)).thenReturn(true);
        assertFalse(new SysDictDataServiceImpl(mapper).checkDictDataUnique(bo));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("dict_value ="), sql);
        assertEquals(edit, sql.contains("dict_code <>"));
        var values = query.getParamNameValuePairs().values();
        assertTrue(values.contains("open"));
        assertEquals(edit, values.contains(27L));
        assertTrue(sql.contains("dict_type =") && values.contains("status"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkDictTypeUniqueExcludesEditedRecord(boolean edit) {
        var mapper = mock(SysDictTypeMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "DictType"), SysDictType.class);
        when(mapper.currentModelClass()).thenReturn(SysDictType.class);
        var query = new LambdaCrudChainWrapper<SysDictType, SysDictTypeVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var bo = new SysDictTypeBo();
        bo.setDictType("status");
        bo.setDictId(edit ? 27L : null);
        when(mapper.exists(query)).thenReturn(true);
        assertFalse(new SysDictTypeServiceImpl(mapper, mock()).checkDictTypeUnique(bo));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("dict_type ="), sql);
        assertEquals(edit, sql.contains("dict_id <>"));
        var values = query.getParamNameValuePairs().values();
        assertTrue(values.contains("status"));
        assertEquals(edit, values.contains(27L));
    }
}
