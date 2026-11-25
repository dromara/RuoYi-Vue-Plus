package org.dromara.system.controller.system;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysSocial;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.mapper.SysSocialMapper;
import org.dromara.system.service.impl.SysSocialServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysSocialControllerTest {
    @Test
    void listsBindingsForCurrentUser() {
        var mapper = mock(SysSocialMapper.class);
        when(mapper.currentModelClass()).thenReturn(SysSocial.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "social"), SysSocial.class);
        var query = new LambdaCrudChainWrapper<SysSocial, SysSocialVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var binding = new SysSocialVo();
        binding.setUserId(21L);
        when(mapper.selectVoList(query)).thenReturn(List.of(binding));
        var controller = new SysSocialController(new SysSocialServiceImpl(mapper));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(21L);
            assertEquals(List.of(binding), controller.list().getData());
            assertTrue(query.getSqlSegment().contains("user_id ="));
            assertEquals(List.of(21L), List.copyOf(query.getParamNameValuePairs().values()));
        }
    }
}
