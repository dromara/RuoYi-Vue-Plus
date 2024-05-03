package org.dromara.datasource.jdbc.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DbType {
    /**
     * MYSQL
     */
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", "org.dromara.datasource.jdbc.query.MysqlJdbcTemplate", "MySql数据库"),
    /**
     * MARIADB
     */
    MARIADB("mariadb", "org.mariadb.jdbc.Driver", "org.dromara.datasource.jdbc.query.MysqlJdbcTemplate", "MariaDB数据库"),
    /**
     * ORACLE
     */
    ORACLE("oracle", "oracle.jdbc.OracleDriver", "", "Oracle11g及以下数据库(高版本推荐使用ORACLE_NEW)"),
    /**
     * oracle12c new pagination
     */
    ORACLE_12C("oracle12c", "oracle.jdbc.OracleDriver", "", "Oracle12c+数据库"),
    /**
     * DB2
     */
    DB2("db2", "com.ibm.db2.jcc.DB2Driver", "", "DB2数据库"),
    /**
     * H2
     */
    H2("h2", "org.h2.Driver", "", "H2数据库"),
    /**
     * HSQL
     */
    HSQL("hsql", "org.hsqldb.jdbc.JDBCDriver", "", "HSQL数据库"),
    /**
     * SQLITE
     */
    SQLITE("sqlite", "org.sqlite.JDBC", "", "SQLite数据库"),
    /**
     * POSTGRE
     */
    POSTGRE_SQL("postgresql", "org.postgresql.Driver", "", "Postgre数据库"),
    /**
     * SQLSERVER2005
     */
    SQL_SERVER2005("sqlserver2005", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "", "SQLServer2005数据库"),
    /**
     * SQLSERVER
     */
    SQL_SERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "", "SQLServer数据库"),
    /**
     * DM
     */
    DM("dm", "dm.jdbc.driver.DmDriver", "", "达梦数据库"),
    /**
     * xugu
     */
    XU_GU("xugu", "com.xugu.cloudjdbc.Driver", "", "虚谷数据库"),
    /**
     * Kingbase
     */
    KINGBASE_ES("kingbasees", "cn.kingbase.es.mapreduce.jdbc.KingbaseDriver", "", "人大金仓数据库"),
    /**
     * Phoenix
     */
    PHOENIX("phoenix", "org.apache.phoenix.jdbc.PhoenixDriver", "", "Phoenix HBase数据库"),
    /**
     * Gauss
     */
    GAUSS("zenith", "com.huawei.gauss.jdbc.ZenithDriver", "", "Gauss 数据库"),
    /**
     * ClickHouse
     */
    CLICK_HOUSE("clickhouse", "ru.yandex.clickhouse.ClickHouseDriver", "org.dromara.datasource.jdbc.query.ClickHouseJdbcTemplate", "clickhouse 数据库"),
    /**
     * GBase
     */
    GBASE("gbase", "com.gbase.jdbc.Driver", "", "南大通用(华库)数据库"),
    /**
     * GBase-8s
     */
    GBASE_8S("gbase-8s", "com.gbase.jdbc.Driver", "", "南大通用数据库 GBase 8s"),
    /**
     * use {@link  #GBASE_8S}
     *
     * @deprecated 2022-05-30
     */
    @Deprecated
    GBASEDBT("gbasedbt", "com.gbasedbt.jdbc.Driver", "", "南大通用数据库"),
    /**
     * use {@link  #GBASE_8S}
     *
     * @deprecated 2022-05-30
     */
    @Deprecated
    GBASE_INFORMIX("gbase 8s", "com.gbase.jdbc.Driver", "", "南大通用数据库 GBase 8s"),
    /**
     * GBase8sPG
     */
    GBASE8S_PG("gbase8s-pg", "com.gbasedbt.jdbc.Driver", "", "南大通用数据库 GBase 8s兼容pg"),
    /**
     * GBase8c
     */
    GBASE_8C("gbase8c", "com.gbase.jdbc.Driver", "", "南大通用数据库 GBase 8c"),
    /**
     * Sinodb
     */
    SINODB("sinodb", "com.sinodb.jdbc.Driver", "", "星瑞格数据库"),
    /**
     * Oscar
     */
    OSCAR("oscar", "com.oscar.Driver", "", "神通数据库"),
    /**
     * Sybase
     */
    SYBASE("sybase", "com.sybase.jdbc4.jdbc.SybDriver", "", "Sybase ASE 数据库"),
    /**
     * OceanBase
     */
    OCEAN_BASE("oceanbase", "com.aliyun.oceanbase.jdbc.OceanBaseDriver", "", "OceanBase 数据库"),
    /**
     * Firebird
     */
    FIREBIRD("Firebird", "org.firebirdsql.jdbc.FBDriver", "", "Firebird 数据库"),
    /**
     * HighGo
     */
    HIGH_GO("highgo", "com.highgo.jdbc.Driver", "", "瀚高数据库"),
    /**
     * CUBRID
     */
    CUBRID("cubrid", "cubrid.jdbc.driver.CUBRIDDriver", "", "CUBRID数据库"),
    /**
     * SUNDB
     */
    SUNDB("sundb", "com.unicom.sundb.jdbc.sundbjdbcdriver", "", "SUNDB数据库"),
    /**
     * Hana
     */
    SAP_HANA("hana", "com.sap.db.jdbc.Driver", "", "SAP_HANA数据库"),
    /**
     * Impala
     */
    IMPALA("impala", "org.apache.hive.jdbc.HiveDriver", "", "impala数据库"),
    /**
     * Vertica
     */
    VERTICA("vertica", "com.vertica.jdbc.Driver", "", "vertica数据库"),
    /**
     * xcloud
     */
    XCloud("xcloud", "com.xcloud.jdbc.driver.XCloudDriver", "", "行云数据库"),
    /**
     * redshift
     */
    REDSHIFT("redshift", "com.amazon.redshift.jdbc.Driver", "", "亚马逊redshift数据库"),
    /**
     * openGauss
     */
    OPENGAUSS("openGauss", "org.opengauss.Driver", "", "华为 opengauss 数据库"),
    /**
     * TDengine
     */
    TDENGINE("TDengine", "com.taosdata.jdbc.TSDBDriver", "", "TDengine数据库"),
    /**
     * Informix
     */
    INFORMIX("informix", "com.informix.jdbc.IfxDriver", "", "Informix数据库"),
    /**
     * uxdb
     */
    UXDB("uxdb", "com.uxun.jdbc.Driver", "", "优炫数据库"),
    /**
     * lealone
     */
    LEALONE("lealone", "org.lealone.Driver", "", "Lealone数据库"),
    /**
     * trino
     */
    TRINO("trino", "io.trino.jdbc.TrinoDriver", "", "Trino数据库"),
    /**
     * presto
     */
    PRESTO("presto", "com.facebook.presto.jdbc.PrestoDriver", "", "Presto数据库"),
    /**
     * UNKNOWN DB
     */
    OTHER("other", "", "", "其他数据库");


    /**
     * 数据库名称
     */
    private final String db;
    /**
     * 驱动类
     */
    private final String driverClass;
    /**
     * 查询类
     */
    private final String queryClass;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 获取数据库类型
     *
     * @param dbType 数据库类型字符串
     */
    public static DbType getDbType(String dbType) {
        for (DbType type : DbType.values()) {
            if (type.db.equalsIgnoreCase(dbType)) {
                return type;
            }
        }
        return OTHER;
    }

    public boolean mysqlSameType() {
        return this == DbType.MYSQL
            || this == DbType.MARIADB
            || this == DbType.GBASE
            || this == DbType.OSCAR
            || this == DbType.XU_GU
            || this == DbType.CLICK_HOUSE
            || this == DbType.OCEAN_BASE
            || this == DbType.CUBRID
            || this == DbType.SUNDB;
    }

    public boolean oracleSameType() {
        return this == DbType.ORACLE
            || this == DbType.DM
            || this == DbType.GAUSS;
    }

    public boolean postgresqlSameType() {
        return this == DbType.POSTGRE_SQL
            || this == DbType.H2
            || this == DbType.LEALONE
            || this == DbType.SQLITE
            || this == DbType.HSQL
            || this == DbType.KINGBASE_ES
            || this == DbType.PHOENIX
            || this == DbType.SAP_HANA
            || this == DbType.IMPALA
            || this == DbType.HIGH_GO
            || this == DbType.VERTICA
            || this == DbType.REDSHIFT
            || this == DbType.OPENGAUSS
            || this == DbType.TDENGINE
            || this == DbType.UXDB
            || this == DbType.GBASE8S_PG
            || this == DbType.GBASE_8C;
    }
}
