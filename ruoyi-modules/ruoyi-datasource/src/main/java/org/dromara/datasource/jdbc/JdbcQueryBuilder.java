package org.dromara.datasource.jdbc;

import org.dromara.datasource.jdbc.core.DbType;
import org.dromara.datasource.utils.ExceptionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;

/**
 * jdbc query 构造器
 *
 * @author xyxj xieyangxuejun@gmail.com
 */
public class JdbcQueryBuilder {

    private JdbcQueryBuilder() {
    }

    // 构造者模式
    public static final class Builder {

        private boolean ignoreWarnings = true;

        private int fetchSize = -1;

        private int maxRows = -1;

        private int queryTimeout = -1;

        private boolean skipResultsProcessing = false;

        private boolean skipUndeclaredResults = false;

        private boolean resultsMapCaseInsensitive = false;

        private DataSource dataSource;

        private DbType dbType = DbType.MYSQL;

        public Builder setIgnoreWarnings(boolean ignoreWarnings) {
            this.ignoreWarnings = ignoreWarnings;
            return this;
        }

        public Builder setFetchSize(int fetchSize) {
            this.fetchSize = fetchSize;
            return this;
        }

        public Builder setMaxRows(int maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public Builder setQueryTimeout(int queryTimeout) {
            this.queryTimeout = queryTimeout;
            return this;
        }

        public Builder setSkipResultsProcessing(boolean skipResultsProcessing) {
            this.skipResultsProcessing = skipResultsProcessing;
            return this;
        }

        public Builder setSkipUndeclaredResults(boolean skipUndeclaredResults) {
            this.skipUndeclaredResults = skipUndeclaredResults;
            return this;
        }

        public Builder setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
            this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
            return this;
        }

        public Builder setDbType(DbType dbType) {
            this.dbType = dbType;
            return this;
        }

        /**
         * 必须设置Datasource
         */
        public Builder setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public JdbcQuery build() throws DatabaseException {
            if (dataSource == null) {
                throw ExceptionUtils.create("必须设置DataSource");
            }
            if (dbType == null) {
                throw ExceptionUtils.create("数据类型未空");
            }
            JdbcQuery jdbcQuery;
            try {
                Class<?> clz = Class.forName(dbType.getQueryClass());
                Constructor<?> constructor = clz.getConstructor(DataSource.class);
                jdbcQuery = (JdbcQuery) constructor.newInstance(dataSource);
            } catch (Exception e) {
                throw ExceptionUtils.create("创建JdbcQuery失败:" + e.getMessage());
            }
            if (!ignoreWarnings) jdbcQuery.getJdbc().setIgnoreWarnings(false);
            if (-1 != fetchSize) jdbcQuery.getJdbc().setFetchSize(fetchSize);
            if (-1 != maxRows) jdbcQuery.getJdbc().setMaxRows(maxRows);
            if (-1 != queryTimeout) jdbcQuery.getJdbc().setQueryTimeout(queryTimeout);
            if (skipResultsProcessing) jdbcQuery.getJdbc().setSkipResultsProcessing(true);
            if (skipUndeclaredResults) jdbcQuery.getJdbc().setSkipUndeclaredResults(true);
            if (resultsMapCaseInsensitive) jdbcQuery.getJdbc().setResultsMapCaseInsensitive(true);
            return jdbcQuery;
        }
    }
}
