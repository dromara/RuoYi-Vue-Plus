package org.dromara.workflow.liteflow.instance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spi.spring.SpringAware;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.liteflow.component.NoopComponent;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.warm.flow.orm.mapper.*;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.service.impl.FlwInstanceServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteInstanceChainTest {
    private final FlowInstanceMapper mapper = mock(FlowInstanceMapper.class);
    private final DefService definitions = mock(DefService.class);
    private final InsService instances = mock(InsService.class);
    private final TaskService tasks = mock(TaskService.class);
    private final HisTaskService history = mock(HisTaskService.class);
    private final FlowProcessEventHandler events = mock(FlowProcessEventHandler.class);
    private FlowExecutor executor;
    private final AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext();

    @BeforeEach
    void loadRules() {
        var metadata = new MapperBuilderAssistant(new MybatisConfiguration(), "delete");
        for (var entity : List.of(FlowInstance.class, FlowHisTask.class, FlowTask.class)) {
            TableInfoHelper.initTableInfo(metadata, entity);
        }
        var config = new LiteflowConfig();
        config.setRuleSource("liteflow/instance-chain.el.xml");
        spring.registerBean(LiteflowConfig.class, () -> config);
        spring.register(SpringAware.class);
        spring.refresh();
        executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        FlowBus.addManagedNode("instanceDeleteLoad", new InstanceDeleteLoadComponent(mapper));
        FlowBus.addManagedNode("instanceDeleteExists", new InstanceDeleteExistsComponent());
        FlowBus.addManagedNode("instanceDeleteEvent", new InstanceDeleteEventComponent(definitions, events));
        FlowBus.addManagedNode("instanceDeleteExecute", new InstanceDeleteExecuteComponent(instances,
            mock(FlowHisTaskMapper.class), mock(FlowTaskMapper.class)));
        FlowBus.addManagedNode("noop", new NoopComponent());
        executor.init(true);
    }

    @AfterEach
    void close() {
        FlowBus.removeChain("deleteInstanceChain");
        List.of("instanceDeleteLoad", "instanceDeleteExists", "instanceDeleteEvent", "instanceDeleteExecute", "noop")
            .forEach(FlowBus::removeNode);
        spring.close();
    }

    @ParameterizedTest
    @CsvSource({"business,owner", "running,admin", "history,owner", "history,mixed", "running,missing"})
    void deleteEntryHonorsBatchOwnershipAndPublishesForRemovedInstances(String mode, String scenario) {
        var first = new FlowInstance().setBusinessId("A").setDefinitionId(4L).setCreateBy("7").setId(1L);
        var second = new FlowInstance().setBusinessId("B").setDefinitionId(4L).setCreateBy(
            "owner".equals(scenario) ? "7" : "9").setId(2L);
        var rows = "missing".equals(scenario) ? List.<FlowInstance>of() : List.of(first, second);
        var ids = List.of(1L, 2L);
        when(mapper.selectList(any())).thenReturn(rows);
        when(mapper.selectByIds(ids)).thenReturn(rows);
        var definition = mock(Definition.class);
        when(definition.getId()).thenReturn(4L);
        when(definition.getFlowCode()).thenReturn("leave");
        when(definitions.getByIds(any())).thenReturn(List.of(definition));
        when(instances.remove(ids)).thenReturn(true);
        var service = new FlwInstanceServiceImpl(instances, definitions, tasks, null, mapper, null, null, null);
        try (var beans = mockStatic(SpringUtil.class); var engine = mockStatic(FlowEngine.class);
             var login = mockStatic(LoginHelper.class)) {
            beans.when(() -> SpringUtil.getBean(FlowExecutor.class)).thenReturn(executor);
            login.when(LoginHelper::getUserIdStr).thenReturn("7");
            login.when(LoginHelper::isSuperAdmin).thenReturn("admin".equals(scenario));
            engine.when(FlowEngine::taskService).thenReturn(tasks);
            engine.when(FlowEngine::hisTaskService).thenReturn(history);
            engine.when(FlowEngine::insService).thenReturn(instances);
            if ("mixed".equals(scenario)) {
                assertThrows(ServiceException.class, () -> service.deleteHisByInstanceIds(ids));
                verifyNoInteractions(events, instances, tasks, history);
                return;
            }
            boolean removed = switch (mode) {
                case "business" -> service.deleteByBusinessIds(List.of("A", "B"));
                case "history" -> service.deleteHisByInstanceIds(ids);
                default -> service.deleteByInstanceIds(ids);
            };
            if (rows.isEmpty()) {
                assertFalse(removed);
                verifyNoInteractions(events, instances, tasks, history);
                return;
            }
            assertTrue(removed);
            verify(events).processDeleteHandler("leave", "A");
            verify(events).processDeleteHandler("leave", "B");
            verifyNoMoreInteractions(events);
            if ("history".equals(mode)) {
                verify(tasks).deleteByInsIds(ids);
                verify(history).deleteByInsIds(ids);
                verify(instances).removeByIds(ids);
            } else {
                verify(instances).remove(ids);
            }
        }
    }
}
