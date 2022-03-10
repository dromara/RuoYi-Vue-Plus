package com.ruoyi.common.helper;

import cn.hutool.core.convert.Convert;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.ruoyi.common.enums.DataBaseType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 数据库助手
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataBaseHelper {

    /**
     * 获取当前数据库类型
     */
    public static DataBaseType getDataBasyType() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) SpringUtils.getBean(DataSource.class);
        DataSource dataSource = ds.determineDataSource();
        try {
            DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            return DataBaseType.find(databaseProductName);
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public static boolean isMySql() {
        return DataBaseType.MY_SQL == getDataBasyType();
    }

    public static boolean isOracle() {
        return DataBaseType.ORACLE == getDataBasyType();
    }

    public static boolean isPostgerSql() {
        return DataBaseType.POSTGRE_SQL == getDataBasyType();
    }

    public static boolean isSqlServer() {
        return DataBaseType.SQL_SERVER == getDataBasyType();
    }

    public static String findInSet(Object var1, String var2) {
        DataBaseType dataBasyType = getDataBasyType();
        if (dataBasyType == DataBaseType.SQL_SERVER) {
            return "charindex(" + Convert.toStr(var1) + ", " + var2 + ") <> 0";
        }
        return "find_in_set(" + Convert.toStr(var1) + ", " + var2 + ") <> 0";
    }
}
