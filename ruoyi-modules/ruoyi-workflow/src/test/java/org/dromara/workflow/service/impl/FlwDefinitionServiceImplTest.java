package org.dromara.workflow.service.impl;

import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlwDefinitionServiceImplTest {

    @Test
    void eventNodesDoNotRequireAssignee() {
        for (String eventType : new String[]{"MESSAGE_START", "TIMER_CATCH", "ERROR_BOUNDARY", "TERMINATE_END"}) {
            FlowNode node = new FlowNode()
                .setNodeType(NodeType.BETWEEN.getKey())
                .setNodeCode("event")
                .setEventType(eventType);
            assertFalse(FlwDefinitionServiceImpl.requiresAssignee(node, "apply"), eventType);
        }
    }

    @Test
    void manualApprovalStillRequiresAssignee() {
        FlowNode approval = new FlowNode()
            .setNodeType(NodeType.BETWEEN.getKey())
            .setNodeCode("approve");
        assertTrue(FlwDefinitionServiceImpl.requiresAssignee(approval, "apply"));
        assertFalse(FlwDefinitionServiceImpl.requiresAssignee(approval, "approve"));
    }

    @Test
    void serviceTaskDoesNotRequireAssignee() {
        FlowNode service = new FlowNode()
            .setNodeType(NodeType.SERVICE.getKey())
            .setNodeCode("service");
        assertFalse(FlwDefinitionServiceImpl.requiresAssignee(service, "apply"));
    }
}
