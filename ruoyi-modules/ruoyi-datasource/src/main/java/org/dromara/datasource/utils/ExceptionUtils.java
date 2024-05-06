package org.dromara.datasource.utils;

import org.apache.ibatis.datasource.DataSourceException;

/**
 * ExceptionUtils
 * 异常工具类
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    /**
     * 返回一个新的异常，统一构建，方便统一处理
     *
     * @param msg 消息
     * @param t   异常信息
     * @return 返回异常
     */
    public static DataSourceException create(String msg, Throwable t, Object... params) {
        return new DataSourceException(String.format(msg, params), t);
    }

    /**
     * 重载的方法
     *
     * @param msg 消息
     * @return 返回异常
     */
    public static DataSourceException create(String msg, Object... params) {
        return new DataSourceException(String.format(msg, params));
    }

    /**
     * 重载的方法
     *
     * @param t 异常
     * @return 返回异常
     */
    public static DataSourceException create(Throwable t) {
        return new DataSourceException(t);
    }

    public static void throwOr(boolean condition, String msg, Object... params) throws DataSourceException {
        if (condition) {
            throw create(msg, params);
        }
    }

    /**
     * 方法为实现异常
     */
    public static DataSourceException methodNotImplemented() {
        return create("method not implemented");
    }
}
