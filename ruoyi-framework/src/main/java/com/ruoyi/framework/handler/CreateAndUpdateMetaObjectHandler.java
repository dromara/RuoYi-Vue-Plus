package com.ruoyi.framework.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MP注入处理器
 *
 * @author Lion Li
 * @date 2021/4/25
 */
@Slf4j
public class CreateAndUpdateMetaObjectHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		try {
			if (ObjectUtil.isNotNull(metaObject) && metaObject.getOriginalObject() instanceof BaseEntity) {
				BaseEntity baseEntity = (BaseEntity) metaObject.getOriginalObject();
				Date current = new Date();
				// 创建时间为空 则填充
				if (ObjectUtil.isNull(baseEntity.getCreateTime())) {
					baseEntity.setCreateTime(current);
				}
				// 更新时间为空 则填充
				if (ObjectUtil.isNull(baseEntity.getUpdateTime())) {
					baseEntity.setUpdateTime(current);
				}
				String username = getLoginUsername();
				// 当前已登录 且 创建人为空 则填充
				if (StringUtils.isNotBlank(username)
						&& StringUtils.isBlank(baseEntity.getCreateBy())) {
					baseEntity.setCreateBy(username);
				}
				// 当前已登录 且 更新人为空 则填充
				if (StringUtils.isNotBlank(username)
						&& StringUtils.isBlank(baseEntity.getUpdateBy())) {
					baseEntity.setUpdateBy(username);
				}
			}
		} catch (Exception e) {
			throw new ServiceException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
		}
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		try {
			if (ObjectUtil.isNotNull(metaObject) && metaObject.getOriginalObject() instanceof BaseEntity) {
				BaseEntity baseEntity = (BaseEntity) metaObject.getOriginalObject();
				Date current = new Date();
                // 更新时间填充(不管为不为空)
                baseEntity.setUpdateTime(current);
                String username = getLoginUsername();
                // 当前已登录 更新人填充(不管为不为空)
                if (StringUtils.isNotBlank(username)) {
                    baseEntity.setUpdateBy(username);
                }
			}
		} catch (Exception e) {
			throw new ServiceException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
		}
	}

	/**
	 * 获取登录用户名
	 */
	private String getLoginUsername() {
		LoginUser loginUser;
		try {
			loginUser = SecurityUtils.getLoginUser();
		} catch (Exception e) {
			log.warn("自动注入警告 => 用户未登录");
			return null;
		}
		return loginUser.getUsername();
	}

}
