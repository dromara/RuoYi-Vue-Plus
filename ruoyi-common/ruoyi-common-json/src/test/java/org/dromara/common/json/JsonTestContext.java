package org.dromara.common.json;

import org.dromara.common.core.utils.SpringUtils;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * 为依赖全局 JsonMapper 的测试初始化最小 Spring 容器。
 */
public final class JsonTestContext {

    private static final GenericApplicationContext CONTEXT = createContext();

    private JsonTestContext() {
    }

    /**
     * 触发最小 Spring 容器初始化，供 JsonUtils 获取 JsonMapper。
     */
    public static void initialize() {
        CONTEXT.isActive();
    }

    /**
     * 创建仅注册 JsonMapper 的测试容器。
     *
     * @return 已启动的测试容器
     */
    private static GenericApplicationContext createContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
        context.refresh();
        new SpringUtils().setApplicationContext(context);
        return context;
    }

}
