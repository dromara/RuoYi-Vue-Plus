package org.dromara.workflow.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstanceListFilterTest {
    @ParameterizedTest
    @ValueSource(strings = {"running", "finished", "mine"})
    void listFiltersByBusinessIdAndSelectedScope(String scope) {
        var metadata = new MapperBuilderAssistant(new MybatisConfiguration(), "instance-list");
        for (var entity : List.of(FlowInstance.class, FlowDefinition.class, FlowInstanceBizExt.class)) {
            TableInfoHelper.initTableInfo(metadata, entity);
        }
        var mapper = mock(FlwInstanceMapper.class);
        var service = new FlwInstanceServiceImpl(null, null, null, null, null, null, mapper, null);
        var request = new FlowInstanceBo();
        request.setBusinessId("EXP-4");
        when(mapper.selectInstanceList(any(), any())).thenAnswer(call -> {
            MPJLambdaWrapper<FlowInstance> query = call.getArgument(1);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("fi.business_id ="), sql);
            assertTrue(sql.contains(scope.equals("mine") ? "fi.create_by =" : "fi.flow_status IN"), sql);
            var expected = switch (scope) {
                case "running" -> Set.of("EXP-4", "draft", "waiting", "back", "cancel");
                case "finished" -> Set.of("EXP-4", "finish", "invalid", "termination");
                default -> Set.of("EXP-4", "7");
            };
            assertEquals(expected, Set.copyOf(query.getParamNameValuePairs().values()));
            return new Page<>(1, 10, 4);
        });
        var page = new PageQuery(10, 1);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("7");
            var result = switch (scope) {
                case "running" -> service.selectRunningInstanceList(request, page);
                case "finished" -> service.selectFinishInstanceList(request, page);
                default -> service.selectCurrentInstanceList(request, page);
            };
            assertEquals(4, result.getTotal());
        }
    }
}
