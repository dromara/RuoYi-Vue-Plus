package org.dromara.common.encrypt.interceptor;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.*;
import org.dromara.common.encrypt.core.EncryptedFieldProcessor;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Properties;

/**
 * 入参加密拦截器
 *
 * @author Lion Li
 */
@Intercepts({@Signature(
    type = ParameterHandler.class,
    method = "setParameters",
    args = {PreparedStatement.class})
})
@AllArgsConstructor
public class MybatisEncryptInterceptor implements Interceptor {

    private final EncryptedFieldProcessor encryptedFieldProcessor;

    /**
     * 加密 MyBatis 入参中的加密字段，并在执行后恢复原始值。
     *
     * @param invocation 拦截调用信息
     * @return MyBatis 执行结果
     * @throws Throwable 拦截处理异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        List<EncryptedFieldProcessor.FieldSnapshot> snapshots = List.of();
        Object target = invocation.getTarget();
        if (target instanceof ParameterHandler parameterHandler) {
            Object parameterObject = parameterHandler.getParameterObject();
            if (ObjectUtil.isNotNull(parameterObject) && !(parameterObject instanceof String)) {
                snapshots = encryptedFieldProcessor.encrypt(parameterObject);
            }
        }
        try {
            return invocation.proceed();
        } finally {
            for (EncryptedFieldProcessor.FieldSnapshot snapshot : snapshots) {
                snapshot.restore();
            }
        }
    }

    /**
     * 包装 MyBatis 目标对象。
     *
     * @param target 目标对象
     * @return 包装后的对象
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 设置插件属性。
     *
     * @param properties 插件属性
     */
    @Override
    public void setProperties(Properties properties) {
    }
}
