package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.system.mapper.SysSocialMapper;
import org.dromara.system.domain.SysSocial;
import org.dromara.system.domain.bo.SysSocialBo;
import org.dromara.system.domain.vo.SysSocialVo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialBindingQueryTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void usesRequestedBindingCriteria(boolean byExternalIdOnly) {
        var mapper = mock(SysSocialMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "social"), SysSocial.class);
        when(mapper.currentModelClass()).thenReturn(SysSocial.class);
        var query = new LambdaCrudChainWrapper<SysSocial, SysSocialVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var result = List.of(new SysSocialVo());
        when(mapper.selectVoList(query)).thenReturn(result);
        var service = new SysSocialServiceImpl(mapper);
        var bo = new SysSocialBo();
        bo.setUserId(9L);
        bo.setSource("github");
        bo.setAuthId("external");
        assertEquals(result, byExternalIdOnly ? service.selectByAuthId("external") : service.queryList(bo));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("auth_id ="));
        assertEquals(!byExternalIdOnly, sql.contains("source ="));
        assertEquals(!byExternalIdOnly, sql.contains("user_id ="));
        assertEquals(byExternalIdOnly ? Set.of("external") : Set.of("external","github",9L),
            Set.copyOf(query.getParamNameValuePairs().values()));
    }
}
