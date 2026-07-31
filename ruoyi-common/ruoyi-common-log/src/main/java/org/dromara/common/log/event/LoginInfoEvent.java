package org.dromara.common.log.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录事件
 *
 * @author Lion Li
 */

@Data
public class LoginInfoEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 登录状态 0成功 1失败
     */
    private String status;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 客户端IP
     */
    private String ip;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 客户端标识
     */
    private String clientId;

    /**
     * 其他参数
     */
    private Object[] args;

}
