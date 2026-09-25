package org.dromara.common.json.utils;

import cn.hutool.core.lang.Dict;
import org.dromara.common.json.JsonTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonUtils 单元测试")
class JsonUtilsTest {

    /**
     * 在 JsonUtils 类初始化前注册其依赖的全局 JsonMapper。
     */
    @BeforeAll
    static void initializeJsonMapper() {
        JsonTestContext.initialize();
    }

    /**
     * 验证普通对象能够完成 JSON 往返转换及空输入处理。
     */
    @Test
    @DisplayName("对象可以完成 JSON 序列化和反序列化")
    void shouldSerializeAndDeserializeObject() {
        TestUser user = new TestUser(1L, "admin");

        String json = JsonUtils.toJsonString(user);
        TestUser result = JsonUtils.parseObject(json, TestUser.class);

        assertEquals(user, result);
        assertNull(JsonUtils.toJsonString(null));
        assertNull(JsonUtils.parseObject("", TestUser.class));
    }

    /**
     * 验证对象 Map、Map 列表和指定类型列表的解析结果。
     */
    @Test
    @DisplayName("解析对象 Map 和对象列表")
    void shouldParseMapsAndArrays() {
        Dict map = JsonUtils.parseMap("{\"name\":\"admin\",\"enabled\":true}");
        List<Dict> maps = JsonUtils.parseArrayMap("[{\"id\":1},{\"id\":2}]");
        List<TestUser> users = JsonUtils.parseArray("[{\"id\":1,\"name\":\"a\"}]", TestUser.class);

        assertEquals("admin", map.getStr("name"));
        assertEquals(2, maps.size());
        assertEquals(new TestUser(1L, "a"), users.getFirst());
        assertTrue(JsonUtils.parseArray("", TestUser.class).isEmpty());
    }

    /**
     * 验证指定敏感字段会从嵌套对象和数组元素中递归移除。
     */
    @Test
    @DisplayName("递归移除对象和数组中的指定字段")
    void shouldRemoveFieldsRecursively() {
        Map<String, Object> value = Map.of(
            "password", "root-secret",
            "profile", Map.of("name", "admin", "password", "profile-secret"),
            "items", List.of(Map.of("password", "item-secret", "value", 1))
        );

        String json = JsonUtils.toJsonStringExcludeFields(value, "password");
        JsonNode node = JsonUtils.getJsonMapper().readTree(json);

        assertFalse(node.has("password"));
        assertFalse(node.get("profile").has("password"));
        assertFalse(node.get("items").get(0).has("password"));
        assertEquals("admin", node.get("profile").get("name").asString());
    }

    /**
     * 验证业务 JSON 仅接受对象和数组，不接受 JSON 标量。
     */
    @Test
    @DisplayName("仅将 JSON 对象或数组识别为业务 JSON")
    void shouldRecognizeOnlyObjectOrArrayJson() {
        assertTrue(JsonUtils.isJson("{\"id\":1}"));
        assertTrue(JsonUtils.isJson("[1,2]"));
        assertFalse(JsonUtils.isJson("1"));
        assertFalse(JsonUtils.isJson("\"text\""));
        assertFalse(JsonUtils.isJson("invalid"));
        assertFalse(JsonUtils.isJson(" "));
    }

    /**
     * 验证对象和数组类型判断不会相互混淆。
     */
    @Test
    @DisplayName("区分 JSON 对象和数组")
    void shouldDistinguishObjectAndArray() {
        assertTrue(JsonUtils.isJsonObject("{}"));
        assertFalse(JsonUtils.isJsonObject("[]"));
        assertTrue(JsonUtils.isJsonArray("[]"));
        assertFalse(JsonUtils.isJsonArray("{}"));
    }

    private record TestUser(Long id, String name) {
    }

}
