package com.ruoyi.framework.aspectj;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.reflect.ReflectUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.web.service.TokenService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 数据过滤处理
 *
 * @author Lion Li
 */
@Aspect
@Component
public class DataScopeAspect {

	/**
	 * 全部数据权限
	 */
	public static final String DATA_SCOPE_ALL = "1";

	/**
	 * 自定数据权限
	 */
	public static final String DATA_SCOPE_CUSTOM = "2";

	/**
	 * 部门数据权限
	 */
	public static final String DATA_SCOPE_DEPT = "3";

	/**
	 * 部门及以下数据权限
	 */
	public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";

	/**
	 * 仅本人数据权限
	 */
	public static final String DATA_SCOPE_SELF = "5";

	/**
	 * 数据权限过滤关键字
	 */
	public static final String DATA_SCOPE = "dataScope";

	// 配置织入点
	@Pointcut("@annotation(com.ruoyi.common.annotation.DataScope)")
	public void dataScopePointCut() {
	}

	@Before("dataScopePointCut()")
	public void doBefore(JoinPoint point) throws Throwable {
		clearDataScope(point);
		handleDataScope(point);
	}

	protected void handleDataScope(final JoinPoint joinPoint) {
		// 获得注解
		DataScope controllerDataScope = getAnnotationLog(joinPoint);
		if (controllerDataScope == null) {
			return;
		}
		// 获取当前的用户
		LoginUser loginUser = SpringUtils.getBean(TokenService.class).getLoginUser(ServletUtils.getRequest());
		if (Validator.isNotNull(loginUser)) {
			SysUser currentUser = loginUser.getUser();
			// 如果是超级管理员，则不过滤数据
			if (Validator.isNotNull(currentUser) && !currentUser.isAdmin()) {
				dataScopeFilter(joinPoint, currentUser, controllerDataScope.deptAlias(),
					controllerDataScope.userAlias(), controllerDataScope.isUser());
			}
		}
	}

	/**
	 * 数据范围过滤
	 *
	 * @param joinPoint 切点
	 * @param user      用户
	 * @param userAlias 别名
	 */
	public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String deptAlias, String userAlias, boolean isUser) {
		StringBuilder sqlString = new StringBuilder();

		// 将 "." 提取出,不写别名为单表查询,写别名为多表查询
		deptAlias = StrUtil.isNotBlank(deptAlias) ? deptAlias + "." : "";
		userAlias = StrUtil.isNotBlank(userAlias) ? userAlias + "." : "";

		for (SysRole role : user.getRoles()) {
			String dataScope = role.getDataScope();
			if (DATA_SCOPE_ALL.equals(dataScope)) {
				sqlString = new StringBuilder();
				break;
			} else if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
				sqlString.append(StrUtil.format(
					" OR {}dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ",
					deptAlias, role.getRoleId()));
			} else if (DATA_SCOPE_DEPT.equals(dataScope)) {
				sqlString.append(StrUtil.format(" OR {}dept_id = {} ",
					deptAlias, user.getDeptId()));
			} else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
				sqlString.append(StrUtil.format(
					" OR {}dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )",
					deptAlias, user.getDeptId(), user.getDeptId()));
			} else if (DATA_SCOPE_SELF.equals(dataScope)) {
				if (isUser) {
					sqlString.append(StrUtil.format(" OR {}user_id = {} ",
						userAlias, user.getUserId()));
				} else {
					// 数据权限为仅本人且没有userAlias别名不查询任何数据
					sqlString.append(" OR 1=0 ");
				}
			}
		}

		if (StrUtil.isNotBlank(sqlString.toString())) {
			putDataScope(joinPoint, sqlString.substring(4));
		}
	}

	/**
	 * 是否存在注解，如果存在就获取
	 */
	private DataScope getAnnotationLog(JoinPoint joinPoint) {
		Signature signature = joinPoint.getSignature();
		MethodSignature methodSignature = (MethodSignature) signature;
		Method method = methodSignature.getMethod();

		if (method != null) {
			return method.getAnnotation(DataScope.class);
		}
		return null;
	}

	/**
	 * 拼接权限sql前先清空params.dataScope参数防止注入
	 */
	private void clearDataScope(final JoinPoint joinPoint) {
		Object params = joinPoint.getArgs()[0];
		if (Validator.isNotNull(params)) {
			putDataScope(joinPoint, "");
		}
	}

	private static void putDataScope(JoinPoint joinPoint, String sql) {
		Object params = joinPoint.getArgs()[0];
		if (Validator.isNotNull(params)) {
			if (params instanceof BaseEntity) {
				BaseEntity baseEntity = (BaseEntity) params;
				baseEntity.getParams().put(DATA_SCOPE, sql);
			} else {
				Map<String, Object> invoke = ReflectUtils.invokeGetter(params, "params");
				invoke.put(DATA_SCOPE, sql);
			}
		}
	}
}
