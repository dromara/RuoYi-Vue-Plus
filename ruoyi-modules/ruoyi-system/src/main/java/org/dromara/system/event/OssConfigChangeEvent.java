package org.dromara.system.event;

/**
 * OSS 配置变更事件。
 *
 * @param configKey    当前配置 key
 * @param oldConfigKey 变更前配置 key
 * @param configJson   当前配置 JSON，为空表示清理缓存
 * @param defaultConfig 是否设置为默认配置
 */
public record OssConfigChangeEvent(
    String configKey,
    String oldConfigKey,
    String configJson,
    boolean defaultConfig
) {

    public static OssConfigChangeEvent save(String configKey, String oldConfigKey, String configJson) {
        return new OssConfigChangeEvent(configKey, oldConfigKey, configJson, false);
    }

    public static OssConfigChangeEvent remove(String configKey) {
        return new OssConfigChangeEvent(configKey, null, null, false);
    }

    public static OssConfigChangeEvent useDefault(String configKey) {
        return new OssConfigChangeEvent(configKey, null, null, true);
    }

}
