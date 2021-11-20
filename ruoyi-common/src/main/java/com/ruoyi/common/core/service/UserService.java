package com.ruoyi.common.core.service;

import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 通用 用户业务
 *
 * @author Lion Li
 */
public interface UserService {

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    SysUser selectUserById(Long userId);

}
