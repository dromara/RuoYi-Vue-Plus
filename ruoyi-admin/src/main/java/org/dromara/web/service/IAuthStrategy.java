package org.dromara.web.service;


import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.web.domain.vo.LoginVo;

import java.util.function.Consumer;

/**
 * 授权策略
 *
 * @author Michelle.Chung
 */
public interface IAuthStrategy {

    String BASE_NAME = "AuthStrategy";

    /**
     * 登录
     *
     * @param body      登录对象
     * @param client    授权管理视图对象
     * @param grantType 授权类型
     * @return 登录验证信息
     */
    static LoginVo login(String body, SysClientVo client, String grantType) {
        // 授权类型和客户端id
        String beanName = grantType + BASE_NAME;
        if (!SpringUtils.containsBean(beanName)) {
            throw new ServiceException("授权类型不正确!");
        }
        IAuthStrategy instance = SpringUtils.getBean(beanName);
        return instance.login(body, client);
    }

    /**
     * 按客户端配置构建统一登录参数。
     *
     * @param client 客户端配置
     * @return Sa-Token 登录参数
     */
    static SaLoginParameter buildLoginParameter(SysClientVo client) {
        return buildLoginParameter(client, null);
    }

    /**
     * 按客户端配置构建统一登录参数，并预留自定义扩展入口。
     *
     * @param client 客户端配置
     * @param customizer 自定义扩展逻辑
     * @return Sa-Token 登录参数
     */
    static SaLoginParameter buildLoginParameter(SysClientVo client, Consumer<SaLoginParameter> customizer) {
        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        model.setExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY, client.getAccessPath());
        model.setExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY, client.getIpWhitelist());
        if (ObjectUtil.isNotNull(customizer)) {
            customizer.accept(model);
        }
        return model;
    }

    /**
     * 登录
     *
     * @param body   登录对象
     * @param client 授权管理视图对象
     * @return 当前策略完成认证后的登录结果
     */
    LoginVo login(String body, SysClientVo client);

}
