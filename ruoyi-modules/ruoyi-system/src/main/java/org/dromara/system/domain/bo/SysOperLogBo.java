package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import org.dromara.common.log.event.OperLogEvent;
import org.dromara.system.domain.SysOperLog;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志记录业务对象 sys_oper_log
 *
 * @author Michelle.Chung
 * @date 2023-02-07
 */

@Data
@AutoMappers({
    @AutoMapper(target = SysOperLog.class, reverseConvertGenerate = false),
    @AutoMapper(target = OperLogEvent.class)
})
public class SysOperLogBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    private Long operId;

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
     * 业务类型数组
     */
    private Integer[] businessTypes;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     */
    private Integer operatorType;

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
     * 操作地点
     */
    private String operLocation;

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

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

}
