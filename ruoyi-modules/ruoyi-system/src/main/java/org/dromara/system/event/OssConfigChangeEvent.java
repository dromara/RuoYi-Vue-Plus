package org.dromara.system.event;

/**
 * OSS 配置变更事件。
 *
 * @param configKey     当前配置 key
 * @param oldConfigKey  变更前配置 key
 * @param configJson    当前配置 JSON，为空表示清理缓存
 * @param action        配置变更操作
 */
public record OssConfigChangeEvent(
    String configKey,
    String oldConfigKey,
    String configJson,
    Action action
) {

    public enum Action {
        SAVE, REMOVE, SET_DEFAULT, CLEAR_DEFAULT
    }

    /**
     * 创建保存 OSS 配置后的变更事件。
     *
     * @param configKey    当前配置 key
     * @param oldConfigKey 变更前配置 key
     * @param configJson   当前配置 JSON
     * @return OSS 配置变更事件
     */
    public static OssConfigChangeEvent save(String configKey, String oldConfigKey, String configJson) {
        return new OssConfigChangeEvent(configKey, oldConfigKey, configJson, Action.SAVE);
    }

    /**
     * 创建删除 OSS 配置后的变更事件。
     *
     * @param configKey 配置 key
     * @return OSS 配置变更事件
     */
    public static OssConfigChangeEvent remove(String configKey) {
        return new OssConfigChangeEvent(configKey, null, null, Action.REMOVE);
    }

    /**
     * 创建切换默认 OSS 配置后的变更事件。
     *
     * @param configKey 默认配置 key
     * @return OSS 配置变更事件
     */
    public static OssConfigChangeEvent useDefault(String configKey) {
        return new OssConfigChangeEvent(configKey, null, null, Action.SET_DEFAULT);
    }

    public static OssConfigChangeEvent clearDefault(String configKey) {
        return new OssConfigChangeEvent(configKey, null, null, Action.CLEAR_DEFAULT);
    }

}
