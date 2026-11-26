package org.dromara.workflow.liteflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowChainRoutingTest {
    @Test
    void moduleRulesRouteEveryBranchInOrder() {
        List<String> actions = List.of("startPrepareRequest", "startResume", "startPrepareInstance", "startExecute",
            "completePrepare", "completeExecute", "taskOpPrepare", "taskOpLoad", "taskOpExecute", "taskOpNotify",
            "instanceDeleteLoad", "instanceDeleteEvent", "instanceDeleteExecute", "noop");
        List<String> conditions = List.of("startExists", "taskOpNeedNotify", "instanceDeleteExists");
        List<String> chains = List.of("startProcessChain", "completeTaskChain", "taskOperationChain", "deleteInstanceChain");
        var spring = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        var config = new LiteflowConfig();
        spring.registerBean(LiteflowConfig.class, () -> config);
        spring.register(com.yomahub.liteflow.spi.spring.SpringAware.class);
        spring.refresh();
        try {
            config.setRuleSource("liteflow/task-chain.el.xml,liteflow/instance-chain.el.xml");
            var executor = new FlowExecutor();
            executor.setLiteflowConfig(config);
            for (String id : actions) {
                FlowBus.addManagedNode(id, new Action());
            }
            for (String id : conditions) {
                FlowBus.addManagedNode(id, new Condition());
            }
            executor.init(true);
            check(executor, "startProcessChain", false, "startPrepareRequest", "startPrepareInstance", "startExecute");
            check(executor, "startProcessChain", true, "startPrepareRequest", "startResume");
            check(executor, "completeTaskChain", true, "completePrepare", "completeExecute");
            check(executor, "taskOperationChain", true, "taskOpPrepare", "taskOpLoad", "taskOpExecute", "taskOpNotify");
            check(executor, "taskOperationChain", false, "taskOpPrepare", "taskOpLoad", "taskOpExecute", "noop");
            check(executor, "deleteInstanceChain", true, "instanceDeleteLoad", "instanceDeleteEvent", "instanceDeleteExecute");
            check(executor, "deleteInstanceChain", false, "instanceDeleteLoad", "noop");
        } finally {
            chains.forEach(FlowBus::removeChain);
            actions.forEach(FlowBus::removeNode);
            conditions.forEach(FlowBus::removeNode);
            spring.close();
        }
    }

    private void check(FlowExecutor executor, String chain, boolean decision, String... expected) {
        var trace = new Trace(decision, new ArrayList<>());
        var result = executor.execute2Resp(chain, null, trace);
        assertTrue(result.isSuccess(), () -> String.valueOf(result.getCause()));
        assertEquals(List.of(expected), trace.actions());
    }

    public record Trace(boolean decision, List<String> actions) {}

    public static class Action extends NodeComponent {
        @Override
        public void process() {
            getContextBean(Trace.class).actions().add(getNodeId());
        }
    }

    public static class Condition extends NodeBooleanComponent {
        @Override
        public boolean processBoolean() {
            return getContextBean(Trace.class).decision();
        }
    }
}
