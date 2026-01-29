package org.dromara.common.redis.handler;

import org.apache.commons.lang3.Strings;
import org.dromara.common.core.utils.StringUtils;
import org.redisson.config.NameMapper;

/**
 * redis缓存key前缀处理
 *
 * @author ye
 * @date 2022/7/14 17:44
 * @since 4.3.0
 */
public class KeyPrefixHandler implements NameMapper {

    private final String keyPrefix;

    public KeyPrefixHandler(String keyPrefix) {
        //前缀为空 则返回空前缀
        this.keyPrefix = StringUtils.isBlank(keyPrefix) ? "" : keyPrefix + ":";
    }

    /**
     * 增加前缀
     */
    @Override
    public String map(String name) {
        if (StringUtils.isNoneBlank(name,keyPrefix) && !Strings.CS.startsWith(name, keyPrefix)) {
            return keyPrefix + name;
        }
        return name;
    }

    /**
     * 去除前缀
     */
    @Override
    public String unmap(String name) {
        if (StringUtils.isNoneBlank(name,keyPrefix) && Strings.CS.startsWith(name, keyPrefix)) {
            return name.substring(keyPrefix.length());
        }
        return name;
    }

}
