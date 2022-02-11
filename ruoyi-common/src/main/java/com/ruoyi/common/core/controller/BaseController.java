package com.ruoyi.common.core.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;

/**
 * web层通用数据处理
 *
 * @author Lion Li
 */
public class BaseController {

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected R<Void> toAjax(int rows) {
        return rows > 0 ? R.ok() : R.fail();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected R<Void> toAjax(boolean result) {
        return result ? R.ok() : R.fail();
    }

    /**
     * 页面跳转
     */
    public String redirect(String url) {
        return StringUtils.format("redirect:{}", url);
    }

    /**
     * 获取用户缓存信息
     */
    public LoginUser getLoginUser() {
        return LoginHelper.getLoginUser();
    }

    /**
     * 获取登录用户id
     */
    public Long getUserId() {
        return LoginHelper.getUserId();
    }

    /**
     * 获取登录部门id
     */
    public Long getDeptId() {
        return LoginHelper.getDeptId();
    }

    /**
     * 获取登录用户名
     */
    public String getUsername() {
        return LoginHelper.getUsername();
    }
}
