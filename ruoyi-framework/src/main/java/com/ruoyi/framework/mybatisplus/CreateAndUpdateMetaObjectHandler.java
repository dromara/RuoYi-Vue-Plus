package com.ruoyi.framework.mybatisplus;

import cn.hutool.core.lang.Validator;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MP注入处理器
 *
 * @author Lion Li
 * @date 2021/4/25
 */
public class CreateAndUpdateMetaObjectHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		try {
			//根据属性名字设置要填充的值
			if (metaObject.hasGetter("createTime")) {
				if (Validator.isEmpty(metaObject.getValue("createTime"))) {
					this.setFieldValByName("createTime", new Date(), metaObject);
				}
			}
			if (metaObject.hasGetter("createBy")) {
				if (Validator.isEmpty(metaObject.getValue("createBy"))) {
					this.setFieldValByName("createBy", getLoginUsername(), metaObject);
				}
			}
		} catch (Exception e) {
			throw new CustomException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
		}
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		try {
			if (metaObject.hasGetter("updateBy")) {
				if (Validator.isEmpty(metaObject.getValue("updateBy"))) {
					this.setFieldValByName("updateBy", getLoginUsername(), metaObject);
				}
			}
			if (metaObject.hasGetter("updateTime")) {
				if (Validator.isEmpty(metaObject.getValue("updateTime"))) {
					this.setFieldValByName("updateTime", new Date(), metaObject);
				}
			}
		} catch (Exception e) {
			throw new CustomException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
		}
	}

	/**
	 * 获取登录用户名
	 */
	private String getLoginUsername() {
		LoginUser loginUser = SecurityUtils.getLoginUser();
		if (Validator.isEmpty(loginUser)) {
			throw new CustomException("用户未登录 => 无法获取用户信息");
		}
		return loginUser.getUsername();
	}

}
