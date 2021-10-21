package com.ruoyi.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.ruoyi.common.core.mybatisplus.methods.InsertAll;
import com.ruoyi.framework.handler.CreateAndUpdateMetaObjectHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

/**
 * mybatis-plus配置类(下方注释有插件介绍)
 *
 * @author Lion Li
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
@MapperScan("${mybatis-plus.mapperPackage}")
public class MybatisPlusConfig {

	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
		// 分页插件
		interceptor.addInnerInterceptor(paginationInnerInterceptor());
		// 乐观锁插件
		interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
		return interceptor;
	}

	/**
	 * 分页插件，自动识别数据库类型
	 */
	public PaginationInnerInterceptor paginationInnerInterceptor() {
		PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
		// 设置最大单页限制数量，默认 500 条，-1 不受限制
		paginationInnerInterceptor.setMaxLimit(-1L);
		// 分页合理化
		paginationInnerInterceptor.setOverflow(true);
		return paginationInnerInterceptor;
	}

	/**
	 * 乐观锁插件
	 */
	public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
		return new OptimisticLockerInnerInterceptor();
	}

	/**
	 * 元对象字段填充控制器
	 */
	@Bean
	public MetaObjectHandler metaObjectHandler() {
		return new CreateAndUpdateMetaObjectHandler();
	}

	/**
	 * sql注入器配置
	 */
	@Bean
	public ISqlInjector sqlInjector() {
		return new DefaultSqlInjector() {
			@Override
			public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
				List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
				methodList.add(new InsertAll());
				return methodList;
			}
		};
	}

	/**
	 * PaginationInnerInterceptor 分页插件，自动识别数据库类型
	 * https://baomidou.com/guide/interceptor-pagination.html
	 * OptimisticLockerInnerInterceptor 乐观锁插件
	 * https://baomidou.com/guide/interceptor-optimistic-locker.html
	 * MetaObjectHandler 元对象字段填充控制器
	 * https://baomidou.com/guide/auto-fill-metainfo.html
	 * ISqlInjector sql注入器
	 * https://baomidou.com/guide/sql-injector.html
	 * BlockAttackInnerInterceptor 如果是对全表的删除或更新操作，就会终止该操作
	 * https://baomidou.com/guide/interceptor-block-attack.html
	 * IllegalSQLInnerInterceptor sql性能规范插件(垃圾SQL拦截)
	 * IdentifierGenerator 自定义主键策略
	 * https://baomidou.com/guide/id-generator.html
	 * TenantLineInnerInterceptor 多租户插件
	 * https://baomidou.com/guide/interceptor-tenant-line.html
	 * DynamicTableNameInnerInterceptor 动态表名插件
	 * https://baomidou.com/guide/interceptor-dynamic-table-name.html
	 */

}
