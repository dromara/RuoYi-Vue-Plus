package org.dromara.common.log.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志事件
 *
 * @author Lion Li
 */
@Data
public class OperLogEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务模块名称
     */
    private String module;

    /**
     * 操作功能名称
     */
    private String name;

    /**
     * 操作描述（备注）
     */
    private String remark;

    /**
     * 标签（用于检索/分类）
     */
    private String tags;

    /**
     * 业务类型
     */
    private Integer businessType;

    /**
     * 操作渠道
     */
    private Integer channel;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 请求url
     */
    private String operUrl;

    /**
     * 操作地址
     */
    private String operIp;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 请求参数
     */
    private String operParam;

    /**
     * 返回参数
     */
    private String jsonResult;

    /**
     * 操作状态（0正常 1异常）
     */
    private Integer status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    private LocalDateTime operTime;

    /**
     * 消耗时间
     */
    private Long costTime;
}
