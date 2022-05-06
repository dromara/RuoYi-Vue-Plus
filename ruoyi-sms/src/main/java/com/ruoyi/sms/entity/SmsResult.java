package com.ruoyi.sms.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 上传返回体
 *
 * @author Lion Li
 */
@Data
@Builder
public class SmsResult {

    /**
     * 是否成功
     */
    private boolean isSuccess;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 实际响应体
     */
    private Object response;
}
