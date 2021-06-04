package com.ruoyi.web.controller.system;

import com.ruoyi.common.annotation.RedisLock;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.framework.web.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 测试分布式锁的样例
 */
@RestController
@RequestMapping("/system/redisLock")
public class RedisLockController {

	@Autowired
	private TokenService tokenService;

	/**
	 * #p0 标识取第一个参数为redis锁的key
	 * @param loginBody
	 * @return
	 */
	@GetMapping("/getLock")
	@RedisLock(expireTime=10,key = "#p0")
	public AjaxResult getInfo(@RequestBody LoginBody loginBody){
		LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
		SysUser user = loginUser.getUser();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return AjaxResult.success(user);
	}
}
