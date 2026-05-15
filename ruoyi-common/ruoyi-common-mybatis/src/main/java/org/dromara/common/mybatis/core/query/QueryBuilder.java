package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.toolkit.JoinWrappers;

/**
 * MyBatis-Plus 查询构造器入口。
 *
 * @author Lion Li
 */
public final class QueryBuilder {

    private QueryBuilder() {
    }

    /**
     * 创建 Lambda 查询构造辅助对象。
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return Lambda 查询构造辅助对象
     */
    public static <T> LambdaQueryBuilder<T> lambda(Class<T> entityClass) {
        return new LambdaQueryBuilder<>(Wrappers.lambdaQuery(entityClass));
    }

    /**
     * 创建 MPJ Lambda 联表查询构造辅助对象。
     *
     * @param entityClass 主表实体类型
     * @param <T>         主表实体类型
     * @return MPJ Lambda 联表查询构造辅助对象
     */
    public static <T> LambdaJoinQueryBuilder<T> lambdaJoin(Class<T> entityClass) {
        return new LambdaJoinQueryBuilder<>(JoinWrappers.lambda(entityClass));
    }

    /**
     * 创建带主表别名的 MPJ Lambda 联表查询构造辅助对象。
     *
     * @param alias       主表别名
     * @param entityClass 主表实体类型
     * @param <T>         主表实体类型
     * @return MPJ Lambda 联表查询构造辅助对象
     */
    public static <T> LambdaJoinQueryBuilder<T> lambdaJoin(String alias, Class<T> entityClass) {
        return new LambdaJoinQueryBuilder<>(JoinWrappers.lambda(alias, entityClass));
    }

}
