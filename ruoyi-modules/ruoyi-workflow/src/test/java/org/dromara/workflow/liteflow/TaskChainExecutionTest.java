package org.dromara.workflow.liteflow;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yomahub.liteflow.core.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spi.spring.SpringAware;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.liteflow.component.NoopComponent;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.core.workflow.WorkflowServiceImpl;
import org.dromara.warm.flow.core.workflow.result.WorkflowResult;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.warm.flow.orm.mapper.*;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.liteflow.start.*;
import org.dromara.workflow.liteflow.complete.*;
import org.dromara.workflow.liteflow.operation.*;
import org.dromara.workflow.mapper.FlwInstanceBizExtMapper;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.impl.FlwTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskChainExecutionTest {
    @Test
    void serviceReturnsStartResumeCompleteAndTransferOutcomesFromXml() {
        var metadata = new MapperBuilderAssistant(new MybatisConfiguration(), "task-chain");
        TableInfoHelper.initTableInfo(metadata, FlowNode.class);
        TableInfoHelper.initTableInfo(metadata, FlowInstance.class);
        var tasks = mock(TaskService.class);
        var instances = mock(InsService.class);
        var definitions = mock(DefService.class);
        var instanceMapper = mock(FlowInstanceMapper.class);
        var taskMapper = mock(FlowTaskMapper.class);
        var nodeMapper = mock(FlowNodeMapper.class);
        var extensions = mock(FlwInstanceBizExtMapper.class);
        var messages = mock(IFlwCommonService.class);
        var users = mock(org.dromara.system.api.UserService.class);
        var workflow = mock(WorkflowServiceImpl.class);
        var components = Map.ofEntries(
            Map.entry("startPrepareRequest", new StartPrepareRequestComponent(instanceMapper)),
            Map.entry("startExists", new StartExistsComponent()),
            Map.entry("startPrepareInstance", new StartPrepareInstanceComponent()),
            Map.entry("startExecute", new StartExecuteComponent(tasks, extensions)),
            Map.entry("startResume", new StartResumeComponent(tasks, instances, extensions)),
            Map.entry("completePrepare", new CompletePrepareComponent(taskMapper, instances)),
            Map.entry("completeExecute", new CompleteExecuteComponent()),
            Map.entry("taskOpPrepare", new TaskOpPrepareComponent()),
            Map.entry("taskOpLoad", new TaskOpLoadComponent(tasks, nodeMapper)),
            Map.entry("taskOpExecute", new TaskOpExecuteComponent()),
            Map.entry("taskOpNeedNotify", new TaskOpNeedNotifyComponent()),
            Map.entry("taskOpNotify", new TaskOpNotifyComponent(messages, users)),
            Map.entry("noop", new NoopComponent()));
        try (var spring = new AnnotationConfigApplicationContext(); var beans = mockStatic(SpringUtil.class);
             var login = mockStatic(LoginHelper.class); var engine = mockStatic(FlowEngine.class);
             var validation = Validation.buildDefaultValidatorFactory()) {
            var config = new LiteflowConfig();
            config.setRuleSource("liteflow/task-chain.el.xml");
            spring.registerBean(LiteflowConfig.class, () -> config);
            spring.register(SpringAware.class);
            spring.refresh();
            var executor = new FlowExecutor();
            executor.setLiteflowConfig(config);
            components.forEach(FlowBus::addManagedNode);
            executor.init(true);
            beans.when(() -> SpringUtil.getBean(FlowExecutor.class)).thenReturn(executor);
            beans.when(() -> SpringUtil.getBean(Validator.class)).thenReturn(validation.getValidator());
            login.when(LoginHelper::getUserIdStr).thenReturn("7");
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            engine.when(FlowEngine::insService).thenReturn(instances);
            engine.when(FlowEngine::defService).thenReturn(definitions);
            var service = new FlwTaskServiceImpl(tasks, instances, definitions, null, null, taskMapper, null, users,
                null, null, null, nodeMapper, messages, null);
            var instance = spy(new FlowInstance().setBusinessId("A").setFlowStatus("draft").setId(31L));
            doReturn(Map.of()).when(instance).getVariableMap();
            var task = new FlowTask().setDefinitionId(4L).setInstanceId(31L).setNodeCode("apply").setId(11L);
            when(definitions.getPublishByFlowCode("leave")).thenReturn(new FlowDefinition().setId(4L));
            when(instances.getById(31L)).thenReturn(instance);
            when(tasks.list(any())).thenReturn(List.of(task));
            var result = mock(WorkflowResult.class);
            when(result.getInstanceId()).thenReturn(31L);
            when(workflow.start(any())).thenReturn(result);
            var start = new StartProcessBo();
            start.setBusinessId("A");
            start.setFlowCode("leave");
            start.setHandler("7");
            var created = service.startWorkFlow(start);
            assertEquals(31L, created.processInstanceId());
            assertEquals(11L, created.taskId());
            when(instanceMapper.selectOne(any())).thenReturn(instance);
            assertEquals(created, service.startWorkFlow(start));
            verify(workflow).start(any());
            verify(instances).updateById(instance);
            verify(extensions, times(2)).saveOrUpdateByInstanceId(any());
            when(taskMapper.selectById(11L)).thenReturn(task);
            var complete = new CompleteTaskBo();
            complete.setTaskId(11L);
            complete.setHandler("7");
            assertTrue(service.completeTask(complete));
            verify(workflow).complete(argThat(command -> command.getTaskId() == 11L && "7".equals(command.getOperator().getHandler())));
            when(tasks.getById(11L)).thenReturn(task);
            when(nodeMapper.selectOne(any())).thenReturn(new FlowNode());
            var transfer = new TaskOperationBo();
            transfer.setTaskId(11L);
            transfer.setUserId("8");
            transfer.setMessage("转交复核");
            transfer.setMessageType(List.of("1"));
            when(workflow.transfer(any())).thenReturn(result);
            when(result.isSuccess()).thenReturn(false, true);
            assertFalse(service.taskOperation(transfer, "transferTask"));
            verifyNoInteractions(messages);
            var recipients = List.of(new UserDTO());
            when(users.selectListByIds(List.of(8L))).thenReturn(recipients);
            assertTrue(service.taskOperation(transfer, "transferTask"));
            verify(messages).sendMessage(List.of("1"), "转交复核", "单据「转办」提醒", recipients, "/task/taskWaiting");
        } finally {
            List.of("startProcessChain", "completeTaskChain", "taskOperationChain").forEach(FlowBus::removeChain);
            components.keySet().forEach(FlowBus::removeNode);
        }
    }
}
