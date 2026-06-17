package org.dromara.common.mybatis.interceptor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.dromara.common.mybatis.config.properties.SqlLogProperties;

import java.lang.reflect.InvocationTargetException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 完整 SQL 日志拦截器。
 *
 * @author Lion Li
 */
@Slf4j(topic = "SQL_FULL")
@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "queryCursor", args = {Statement.class})
})
public class SqlLogInterceptor implements Interceptor {

    /**
     * 单条日志分片长度。
     */
    private static final int CHUNK_SIZE = 8000;

    /**
     * SQL 空白字符匹配。
     */
    private static final String BLANK_REGEX = "\\s+";

    /**
     * 日期时间格式。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 日期格式。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 时间格式。
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 控制台输出锁，避免多线程 SQL 日志互相穿插。
     */
    private static final ReentrantLock CONSOLE_LOCK = new ReentrantLock();

    /**
     * SQL 日志配置。
     */
    private final SqlLogProperties sqlLogProperties;

    public SqlLogInterceptor(SqlLogProperties sqlLogProperties) {
        this.sqlLogProperties = sqlLogProperties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        BoundSql boundSql = statementHandler.getBoundSql();
        MappedStatement mappedStatement = PluginUtils.mpStatementHandler(statementHandler).mappedStatement();
        long startTime = System.currentTimeMillis();
        try {
            Object result = invocation.proceed();
            printSql(mappedStatement, boundSql, System.currentTimeMillis() - startTime, null);
            return result;
        } catch (Throwable e) {
            printSql(mappedStatement, boundSql, System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    /**
     * 输出 SQL。
     *
     * @param mappedStatement 映射语句
     * @param boundSql        绑定 SQL
     * @param elapsedTime     执行耗时
     * @param throwable       执行异常
     */
    private void printSql(MappedStatement mappedStatement, BoundSql boundSql, long elapsedTime, Throwable throwable) {
        String fullSql = buildFullSql(mappedStatement.getConfiguration(), boundSql);
        String message = buildLogMessage(mappedStatement, elapsedTime, fullSql, throwable);
        printChunk(message);
    }

    /**
     * 构建日志内容。
     *
     * @param mappedStatement 映射语句
     * @param elapsedTime     执行耗时
     * @param fullSql         完整 SQL
     * @param throwable       执行异常
     * @return 日志内容
     */
    private String buildLogMessage(MappedStatement mappedStatement, long elapsedTime, String fullSql, Throwable throwable) {
        if (isLogOutput()) {
            String message = StrUtil.format("Consume Time：{} ms {} Mapper ID：{} Execute SQL：{}",
                elapsedTime, DateUtil.now(), mappedStatement.getId(), fullSql);
            String errorMessage = formatThrowable(throwable);
            if (StrUtil.isNotBlank(errorMessage)) {
                message = message + " Execute Error：" + errorMessage;
            }
            return message;
        }
        String message = StrUtil.format("Consume Time：{} ms {}\nMapper ID：{}\nExecute SQL：{}",
            elapsedTime, DateUtil.now(), mappedStatement.getId(), fullSql);
        String errorMessage = formatThrowable(throwable);
        if (StrUtil.isNotBlank(errorMessage)) {
            message = message + "\nExecute Error：" + errorMessage;
        }
        return message;
    }

    /**
     * 格式化执行异常。
     *
     * @param throwable 执行异常
     * @return 异常信息
     */
    private String formatThrowable(Throwable throwable) {
        if (throwable == null) {
            return StrUtil.EMPTY;
        }
        Throwable realThrowable = unwrapThrowable(throwable);
        String message = realThrowable.getMessage();
        if (StrUtil.isBlank(message)) {
            return StrUtil.EMPTY;
        }
        return realThrowable.getClass().getName() + ": " + message;
    }

    /**
     * 解包反射调用异常。
     *
     * @param throwable 执行异常
     * @return 实际异常
     */
    private Throwable unwrapThrowable(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocationTargetException
            && invocationTargetException.getTargetException() != null) {
            return invocationTargetException.getTargetException();
        }
        return throwable;
    }

    /**
     * 构建完整 SQL。
     *
     * @param configuration MyBatis 配置
     * @param boundSql      绑定 SQL
     * @return 完整 SQL
     */
    private String buildFullSql(Configuration configuration, BoundSql boundSql) {
        String sql = boundSql.getSql().replaceAll(BLANK_REGEX, " ").trim();
        List<String> parameters = buildParameterValues(configuration, boundSql);
        return replacePlaceholders(sql, parameters);
    }

    /**
     * 构建参数值集合。
     *
     * @param configuration MyBatis 配置
     * @param boundSql      绑定 SQL
     * @return 参数值集合
     */
    private List<String> buildParameterValues(Configuration configuration, BoundSql boundSql) {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        List<String> parameters = new ArrayList<>(parameterMappings.size());
        Object parameterObject = boundSql.getParameterObject();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
        for (ParameterMapping parameterMapping : parameterMappings) {
            if (parameterMapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            String propertyName = parameterMapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else if (metaObject != null && metaObject.hasGetter(propertyName)) {
                value = metaObject.getValue(propertyName);
            } else {
                value = null;
            }
            parameters.add(formatParameter(value));
        }
        return parameters;
    }

    /**
     * 替换 SQL 占位符。
     *
     * @param sql        SQL 模板
     * @param parameters 参数集合
     * @return 完整 SQL
     */
    private String replacePlaceholders(String sql, List<String> parameters) {
        if (parameters.isEmpty()) {
            return sql;
        }
        StringBuilder builder = new StringBuilder(sql.length() + parameters.size() * 8);
        int parameterIndex = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (current == '?' && !inSingleQuote && !inDoubleQuote && parameterIndex < parameters.size()) {
                builder.append(parameters.get(parameterIndex++));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    /**
     * 格式化参数值。
     *
     * @param value 参数值
     * @return SQL 参数文本
     */
    private String formatParameter(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Date date) {
            return quote(DateUtil.formatDateTime(date));
        }
        if (value instanceof LocalDateTime localDateTime) {
            return quote(localDateTime.format(DATE_TIME_FORMATTER));
        }
        if (value instanceof LocalDate localDate) {
            return quote(localDate.format(DATE_FORMATTER));
        }
        if (value instanceof LocalTime localTime) {
            return quote(localTime.format(TIME_FORMATTER));
        }
        if (value instanceof TemporalAccessor) {
            return quote(value.toString());
        }
        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name());
        }
        return quote(value.toString());
    }

    /**
     * 包装字符串参数。
     *
     * @param value 字符串值
     * @return SQL 字符串参数
     */
    private String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * 分片输出日志，避免日志链路截断超长 SQL。
     *
     * @param message 日志内容
     */
    private void printChunk(String message) {
        if (!isLogOutput()) {
            printConsole(message);
            return;
        }
        if (message.length() <= CHUNK_SIZE) {
            print(message);
            return;
        }
        String sqlLogId = UUID.randomUUID().toString();
        int total = (message.length() + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (int i = 0; i < total; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, message.length());
            print(StrUtil.format("sqlLogId={} part={}/{} {}", sqlLogId, i + 1, total, message.substring(start, end)));
        }
    }

    /**
     * 输出 SQL 日志。
     *
     * @param message 日志内容
     */
    private void print(String message) {
        if (isLogOutput()) {
            log.info(message);
            return;
        }
        printConsole(message);
    }

    /**
     * 输出控制台日志。
     *
     * @param message 日志内容
     */
    private void printConsole(String message) {
        CONSOLE_LOCK.lock();
        try {
            System.err.println(message);
            System.err.println();
        } finally {
            CONSOLE_LOCK.unlock();
        }
    }

    /**
     * 是否使用日志系统输出。
     *
     * @return true 使用日志系统输出，false 使用控制台输出
     */
    private boolean isLogOutput() {
        return StrUtil.equalsIgnoreCase("log", sqlLogProperties.getOutput());
    }

}
