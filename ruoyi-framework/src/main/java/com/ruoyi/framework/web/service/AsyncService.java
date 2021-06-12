package com.ruoyi.framework.web.service;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.system.domain.SysLogininfor;
import com.ruoyi.system.domain.SysOperLog;
import com.ruoyi.system.service.ISysLogininforService;
import com.ruoyi.system.service.ISysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 异步工厂（产生任务用）
 *
 * @author Lion Li
 */
@Slf4j(topic = "sys-user")
@Async
@Component
public class AsyncService {

	@Autowired
	private ISysLogininforService iSysLogininforService;

	@Autowired
	private ISysOperLogService iSysOperLogService;

	/**
	 * 记录登录信息
	 *
	 * @param username 用户名
	 * @param status   状态
	 * @param message  消息
	 * @param args     列表
	 */
	public void recordLogininfor(final String username, final String status, final String message,
								 HttpServletRequest request, final Object... args) {
		final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
		final String ip = ServletUtils.getClientIP(request);

		String address = AddressUtils.getRealAddressByIP(ip);
		StringBuilder s = new StringBuilder();
		s.append(getBlock(ip));
		s.append(address);
		s.append(getBlock(username));
		s.append(getBlock(status));
		s.append(getBlock(message));
		// 打印信息到日志
		log.info(s.toString(), args);
		// 获取客户端操作系统
		String os = userAgent.getOs().getName();
		// 获取客户端浏览器
		String browser = userAgent.getBrowser().getName();
		// 封装对象
		SysLogininfor logininfor = new SysLogininfor();
		logininfor.setUserName(username);
		logininfor.setIpaddr(ip);
		logininfor.setLoginLocation(address);
		logininfor.setBrowser(browser);
		logininfor.setOs(os);
		logininfor.setMsg(message);
		// 日志状态
		if (Constants.LOGIN_SUCCESS.equals(status) || Constants.LOGOUT.equals(status)) {
			logininfor.setStatus(Constants.SUCCESS);
		} else if (Constants.LOGIN_FAIL.equals(status)) {
			logininfor.setStatus(Constants.FAIL);
		}
		// 插入数据
		iSysLogininforService.insertLogininfor(logininfor);
	}

	/**
	 * 操作日志记录
	 *
	 * @param operLog 操作日志信息
	 */
	public void recordOper(final SysOperLog operLog) {
		// 远程查询操作地点
		operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
		iSysOperLogService.insertOperlog(operLog);
	}

	private String getBlock(Object msg) {
		if (msg == null) {
			msg = "";
		}
		return "[" + msg.toString() + "]";
	}
}
