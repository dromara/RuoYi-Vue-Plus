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

    /**
     * 解密 MyBatis 查询结果中的加密字段。
     *
     * @param invocation 拦截调用信息
     * @return 查询结果
     * @throws Throwable 拦截处理异常
     */
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
