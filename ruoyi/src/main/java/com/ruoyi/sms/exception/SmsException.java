package com.ruoyi.sms.exception;

/**
 * Sms异常类
 *
 * @author Lion Li
 */
public class SmsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SmsException(String msg) {
        super(msg);
    }

}
