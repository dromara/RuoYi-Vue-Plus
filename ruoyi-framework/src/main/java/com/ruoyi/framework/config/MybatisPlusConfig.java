package com.ruoyi.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.incrementer.H2KeyGenerator;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
public class MybatisPlusConfig {

	/**
	 * 分页插件，自动识别数据库类型
	 * https://baomidou.com/guide/interceptor-pagination.html
	 */
	@Bean
	public PaginationInnerInterceptor paginationInnerInterceptor() {
		PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
		// 设置数据库类型为mysql
		paginationInnerInterceptor.setDbType(DbType.MYSQL);
		// 设置最大单页限制数量，默认 500 条，-1 不受限制
		paginationInnerInterceptor.setMaxLimit(-1L);
		return paginationInnerInterceptor;
	}

	/**
	 * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor = false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
	 */
	@Bean
	public ConfigurationCustomizer configurationCustomizer() {
		return configuration -> configuration.setUseDeprecatedExecutor(false);
	}

	/**
	 * 乐观锁插件
	 * https://baomidou.com/guide/interceptor-optimistic-locker.html
	 */
	@Bean
	public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
		return new OptimisticLockerInnerInterceptor();
	}

	/**
	 * 如果是对全表的删除或更新操作，就会终止该操作
	 * https://baomidou.com/guide/interceptor-block-attack.html
	 */
	@Bean
	public BlockAttackInnerInterceptor blockAttackInnerInterceptor() {
		return new BlockAttackInnerInterceptor();
	}

	/**
	 * sql性能规范插件(垃圾SQL拦截)
	 * 如有需要可以启用
	 */
//	@Bean
//	public IllegalSQLInnerInterceptor illegalSQLInnerInterceptor() {
//		return new IllegalSQLInnerInterceptor();
//	}

	/**
	 * Sequence主键策略 IdType.INPUT 时使用
	 * 内置支持：
	 *
	 * DB2KeyGenerator
	 * H2KeyGenerator
	 * KingbaseKeyGenerator
	 * OracleKeyGenerator
	 * PostgreKeyGenerator
	 * https://baomidou.com/guide/sequence.html
	 */
//	@Bean
//	public IKeyGenerator keyGenerator() {
//		return new H2KeyGenerator();
//	}


	/**
	 * 自定义主键策略
	 * https://baomidou.com/guide/id-generator.html
	 */
//	@Bean
//	public IdentifierGenerator idGenerator() {
//		return new CustomIdGenerator();
//	}

	/**
	 * 元对象字段填充控制器
	 * https://baomidou.com/guide/auto-fill-metainfo.html
	 */
//	@Bean
//	public MetaObjectHandler metaObjectHandler() {
//		return new MyMetaObjectHandler();
//	}

	/**
	 * sql注入器配置
	 * https://baomidou.com/guide/sql-injector.html
	 */
//	@Bean
//	public ISqlInjector sqlInjector() {
//		return new DefaultSqlInjector();
//	}

	/**
	 * TenantLineInnerInterceptor 多租户插件
	 * https://baomidou.com/guide/interceptor-tenant-line.html
	 * DynamicTableNameInnerInterceptor 动态表名插件
	 * https://baomidou.com/guide/interceptor-dynamic-table-name.html
	 */

}
