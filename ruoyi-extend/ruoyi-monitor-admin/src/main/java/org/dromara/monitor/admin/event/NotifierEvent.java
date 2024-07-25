package org.dromara.monitor.admin.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知事件
 *
 * @author AprilWind
 */
@Data
public class NotifierEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例注册名称
     */
    private String registName;

    /**
     * 实例状态名称
     */
    private String statusName;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 实例状态
     */
    private String status;

    /**
     * 服务URL
     */
    private String serviceUrl;

}
