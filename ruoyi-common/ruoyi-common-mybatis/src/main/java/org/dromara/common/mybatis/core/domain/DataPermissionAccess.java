package org.dromara.common.mybatis.core.domain;

import cn.hutool.core.collection.CollUtil;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 当前请求的数据权限访问上下文
 *
 * @author Lion Li
 */
public record DataPermissionAccess(Set<String> perms, Set<String> roleKeys) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final DataPermissionAccess EMPTY = new DataPermissionAccess(Set.of(), Set.of());

    public boolean constrained() {
        return CollUtil.isNotEmpty(perms) || CollUtil.isNotEmpty(roleKeys);
    }
}
