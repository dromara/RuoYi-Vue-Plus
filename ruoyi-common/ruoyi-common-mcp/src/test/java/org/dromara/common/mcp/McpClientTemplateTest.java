package org.dromara.common.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.dromara.common.mcp.core.McpClientTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpClientTemplateTest {

    @Test
    void shouldReturnEmptyWhenNoServerNameIsSupplied() {
        assertTrue(template.findClient(null).isEmpty());
        assertTrue(template.callTool(null, "lookup", Map.of()).isEmpty());
        assertTrue(template.readResource(null, "catalog://items").isEmpty());
    }

    private final McpSyncClient client = mock();
    private final McpClientTemplate template = new McpClientTemplate(List.of(client));

    McpClientTemplateTest() {
        when(client.getServerCapabilities()).thenReturn(McpSchema.ServerCapabilities.builder().build());
    }

    @Test
    void shouldListNoToolsForServerWithoutToolCapability() {
        assertEquals(Map.of("unknown", List.of()), template.listTools());
        verify(client, never()).listTools();
    }

    @Test
    void shouldSkipUnsupportedCallsDuringToolBroadcast() {
        assertTrue(template.callTool("lookup", Map.of()).isEmpty());
        verify(client, never()).callTool(any());
    }

    @Test
    void shouldSkipUnsupportedServersDuringResourceRead() {
        assertTrue(template.readResource("catalog://items").isEmpty());
        verify(client, never()).readResource(any(McpSchema.ReadResourceRequest.class));
    }

    @Test
    void shouldContinueToCapableServerInMixedBatch() {
        McpSyncClient capable = mock();
        when(capable.getServerInfo()).thenReturn(new McpSchema.Implementation("catalog", "1"));
        when(capable.getServerCapabilities()).thenReturn(McpSchema.ServerCapabilities.builder()
            .tools(false).resources(false, false).build());
        var listed = mock(McpSchema.ListToolsResult.class);
        var tool = mock(McpSchema.Tool.class);
        when(tool.name()).thenReturn("lookup");
        when(listed.tools()).thenReturn(List.of(tool));
        when(capable.listTools()).thenReturn(listed);
        when(capable.callTool(any())).thenReturn(mock(McpSchema.CallToolResult.class));
        when(capable.readResource(any(McpSchema.ReadResourceRequest.class))).thenReturn(mock(McpSchema.ReadResourceResult.class));
        var mixed = new McpClientTemplate(List.of(client, capable));
        assertEquals(Map.of("unknown", List.of(), "catalog", List.of("lookup")), mixed.listTools());
        assertEquals(List.of("catalog"), List.copyOf(mixed.callTool("lookup", Map.of()).keySet()));
        assertEquals(List.of("catalog"), List.copyOf(mixed.readResource("catalog://items").keySet()));
        assertTrue(mixed.callTool("unknown", "lookup", Map.of()).isEmpty());
        assertTrue(mixed.readResource("unknown", "catalog://items").isEmpty());
    }
}
