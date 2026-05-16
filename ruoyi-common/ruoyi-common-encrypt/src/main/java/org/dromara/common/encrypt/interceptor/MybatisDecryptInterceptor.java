package org.dromara.common.encrypt.interceptor;

import lombok.AllArgsConstructor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.dromara.common.encrypt.core.EncryptedFieldProcessor;

import java.sql.Statement;
import java.util.Properties;

/**
 * 出参解密拦截器
 *
 * @author Lion Li
 */
@Intercepts({@Signature(
    type = ResultSetHandler.class,
    method = "handleResultSets",
    args = {Statement.class})
})
@AllArgsConstructor
public class MybatisDecryptInterceptor implements Interceptor {

    private final EncryptedFieldProcessor encryptedFieldProcessor;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取执行mysql执行结果
        Object result = invocation.proceed();
        if (result == null) {
            return null;
        }
        encryptedFieldProcessor.decrypt(result);
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
