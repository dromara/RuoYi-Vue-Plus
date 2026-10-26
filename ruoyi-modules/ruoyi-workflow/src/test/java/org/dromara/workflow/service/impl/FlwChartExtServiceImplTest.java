package org.dromara.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.dromara.common.core.service.DictService;
import org.dromara.system.api.DeptService;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlwChartExtServiceImplTest {

    @Test
    void showsCurrentAssigneeWithoutApprovalHistory() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "test"), FlowUser.class);
        UserService userService = mock(UserService.class);
        DeptService deptService = mock(DeptService.class);
        FlowHisTaskMapper historyMapper = mock(FlowHisTaskMapper.class);
        FlowTaskMapper taskMapper = mock(FlowTaskMapper.class);
        FlowUserMapper flowUserMapper = mock(FlowUserMapper.class);
        DictService dictService = mock(DictService.class);
        FlwChartExtServiceImpl service = new FlwChartExtServiceImpl(
            userService, deptService, historyMapper, taskMapper, flowUserMapper, dictService);
        ReflectionTestUtils.setField(service, "nodeTooltip", true);

        NodeJson node = new NodeJson().setNodeCode("approve").setNodeName("部门审批");
        DefJson definition = new DefJson()
            .setFlowName("请假申请")
            .setInstance(new FlowInstance().setId(1L))
            .setNodeList(List.of(node));
        service.initPromptContent(definition);

        when(historyMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of(
            new FlowTask().setId(10L).setNodeCode("approve")));
        when(flowUserMapper.selectList(any())).thenReturn(List.of(
            new FlowUser().setAssociated(10L).setType("1").setProcessedBy("7")));
        UserDTO user = new UserDTO();
        user.setUserId(7L);
        user.setDeptId(3L);
        user.setNickName("张三");
        user.setUserName("zhangsan");
        when(userService.selectListByIds(any())).thenReturn(List.of(user));
        when(deptService.selectDeptNamesByIds(any())).thenReturn(Map.of(3L, "研发部"));
        when(dictService.getAllDictByDictType(any())).thenReturn(Map.of());

        service.execute(definition);

        var info = node.getPromptContent().getInfo();
        assertTrue(info.stream().anyMatch(item -> item.getPrefix().contains("张三")
            && item.getPrefix().contains("研发部")));
        assertTrue(info.stream().anyMatch(item -> "用户账号: ".equals(item.getPrefix())
            && "zhangsan".equals(item.getContent())));
        assertTrue(info.stream().anyMatch(item -> "审批状态: ".equals(item.getPrefix())
            && "待审批".equals(item.getContent())));
        verify(userService).selectListByIds(argThat(ids -> ids.size() == 1 && ids.contains(7L)));
        verify(deptService).selectDeptNamesByIds(argThat(ids -> ids.size() == 1 && ids.contains(3L)));
        assertEquals(4, info.size());
    }
}
