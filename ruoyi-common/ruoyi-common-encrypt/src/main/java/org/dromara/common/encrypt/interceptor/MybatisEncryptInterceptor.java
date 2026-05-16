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

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
