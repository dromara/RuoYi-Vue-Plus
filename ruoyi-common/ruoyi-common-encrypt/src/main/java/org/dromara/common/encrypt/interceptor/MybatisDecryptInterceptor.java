package org.dromara.common.encrypt.interceptor;

import lombok.AllArgsConstructor;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.dromara.common.encrypt.core.EncryptedFieldProcessor;

import java.io.IOException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Properties;

/**
 * 出参解密拦截器
 *
 * @author Lion Li
 */
@Intercepts({
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}),
    // MP 3.5.17 起 selectOne/selectVoOne 走游标，需一并拦截解密
    @Signature(type = ResultSetHandler.class, method = "handleCursorResultSets", args = {Statement.class})
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
        Object result = invocation.proceed();
        if (result == null) {
            return null;
        }
        if (result instanceof Cursor<?> cursor) {
            return new DecryptCursor<>(cursor, encryptedFieldProcessor);
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

    /**
     * 查询结果解密游标，保持 MyBatis 游标的惰性读取语义。
     *
     * @param <T> 查询结果类型
     */
    private record DecryptCursor<T>(Cursor<T> delegate, EncryptedFieldProcessor encryptedFieldProcessor)
        implements Cursor<T> {

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public boolean isConsumed() {
            return delegate.isConsumed();
        }

        @Override
        public int getCurrentIndex() {
            return delegate.getCurrentIndex();
        }

        @Override
        public Iterator<T> iterator() {
            Iterator<T> iterator = delegate.iterator();
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public T next() {
                    T result = iterator.next();
                    encryptedFieldProcessor.decrypt(result);
                    return result;
                }
            };
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
