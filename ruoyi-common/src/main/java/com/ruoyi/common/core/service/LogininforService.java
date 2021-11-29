package com.ruoyi.common.core.service;

import javax.servlet.http.HttpServletRequest;

/**
 * 通用 系统访问日志
 *
 * @author Lion Li
 */
public interface LogininforService {

    void recordLogininfor(String username, String status, String message,
                          HttpServletRequest request, Object... args);
}
