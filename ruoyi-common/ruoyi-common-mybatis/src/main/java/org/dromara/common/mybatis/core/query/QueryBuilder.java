package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

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

}
