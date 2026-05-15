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

    public static <T> LambdaQueryBuilder<T> lambda(Class<T> entityClass) {
        return new LambdaQueryBuilder<>(Wrappers.lambdaQuery(entityClass));
    }

}
