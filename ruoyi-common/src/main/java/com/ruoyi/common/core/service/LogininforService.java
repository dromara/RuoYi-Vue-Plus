package com.ruoyi.common.core.service;

import javax.servlet.http.HttpServletRequest;

public interface LogininforService {

    void recordLogininfor(String username, String status, String message,
                          HttpServletRequest request, Object... args);
}
