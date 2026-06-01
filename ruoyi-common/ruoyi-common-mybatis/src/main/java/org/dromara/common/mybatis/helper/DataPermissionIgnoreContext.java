package org.dromara.common.mybatis.helper;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.reflect.ReflectUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据权限忽略状态适配器。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DataPermissionIgnoreContext {

    /**
     * 数据权限忽略状态栈，用于支持嵌套忽略并恢复进入前状态。
     */
    private static final ThreadLocal<Deque<Boolean>> DATA_PERMISSION_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 开启忽略数据权限。
     */
    static void enable() {
        IgnoreStrategy ignoreStrategy = getIgnoreStrategy();
        DATA_PERMISSION_STACK.get().push(ignoreStrategy != null && Boolean.TRUE.equals(ignoreStrategy.getDataPermission()));
        if (ObjectUtil.isNull(ignoreStrategy)) {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).build());
        } else {
            ignoreStrategy.setDataPermission(true);
        }
    }

    /**
     * 关闭忽略数据权限，并恢复进入前的数据权限忽略状态。
     */
    static void disable() {
        Deque<Boolean> stack = DATA_PERMISSION_STACK.get();
        boolean previousDataPermission = !stack.isEmpty() && stack.pop();
        IgnoreStrategy ignoreStrategy = getIgnoreStrategy();
        if (ObjectUtil.isNotNull(ignoreStrategy)) {
            if (previousDataPermission) {
                ignoreStrategy.setDataPermission(true);
            } else if (isOnlyDataPermissionIgnored(ignoreStrategy) && stack.isEmpty()) {
                InterceptorIgnoreHelper.clearIgnoreStrategy();
            } else {
                ignoreStrategy.setDataPermission(false);
            }
        }
        if (stack.isEmpty()) {
            DATA_PERMISSION_STACK.remove();
        }
    }

    /**
     * 获取 MyBatis-Plus 当前线程中的拦截器忽略策略。
     *
     * @return 当前忽略策略，未设置时返回 null
     */
    private static IgnoreStrategy getIgnoreStrategy() {
        Object ignoreStrategyLocal = ReflectUtils.getStaticFieldValue(ReflectUtils.getField(InterceptorIgnoreHelper.class, "IGNORE_STRATEGY_LOCAL"));
        if (ignoreStrategyLocal instanceof ThreadLocal<?> ignoreStrategyThreadLocal
            && ignoreStrategyThreadLocal.get() instanceof IgnoreStrategy ignoreStrategy) {
            return ignoreStrategy;
        }
        return null;
    }

    /**
     * 判断当前忽略策略是否只忽略了数据权限插件。
     *
     * @param ignoreStrategy 忽略策略
     * @return true 仅忽略数据权限 false 还忽略了其他插件能力
     */
    private static boolean isOnlyDataPermissionIgnored(IgnoreStrategy ignoreStrategy) {
        return !Boolean.TRUE.equals(ignoreStrategy.getDynamicTableName())
            && !Boolean.TRUE.equals(ignoreStrategy.getBlockAttack())
            && !Boolean.TRUE.equals(ignoreStrategy.getIllegalSql())
            && !Boolean.TRUE.equals(ignoreStrategy.getTenantLine())
            && CollectionUtil.isEmpty(ignoreStrategy.getOthers());
    }

}
