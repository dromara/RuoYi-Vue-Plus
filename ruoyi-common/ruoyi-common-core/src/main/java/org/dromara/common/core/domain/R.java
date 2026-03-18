package org.dromara.common.core.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.common.core.constant.HttpStatus;

import java.io.Serial;
import java.io.Serializable;

import static org.dromara.common.core.constant.HttpStatus.ERROR;
import static org.dromara.common.core.constant.HttpStatus.SUCCESS;

/**
 * 响应信息主体
 *
 * @param <T> 响应数据的泛型类型
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应提示信息
     */
    private String msg;

    /**
     * 响应业务数据
     */
    private T data;

    /**
     * 构建成功响应结果
     *
     * @param <T> 响应数据的泛型类型
     * @return 成功响应结果对象
     */
    public static <T> R<T> ok() {
        return restResult(null, SUCCESS, "操作成功");
    }

    /**
     * 构建成功响应结果（带业务数据）
     *
     * @param data 业务数据
     * @param <T>  响应数据的泛型类型
     * @return 成功响应结果对象
     */
    public static <T> R<T> ok(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    /**
     * 构建成功响应结果（自定义提示信息）
     *
     * @param msg 自定义提示信息
     * @param <T> 响应数据的泛型类型
     * @return 成功响应结果对象
     */
    public static <T> R<T> ok(String msg) {
        return restResult(null, SUCCESS, msg);
    }

    /**
     * 构建成功响应结果（自定义提示信息+业务数据）
     *
     * @param msg  自定义提示信息
     * @param data 业务数据
     * @param <T>  响应数据的泛型类型
     * @return 成功响应结果对象
     */
    public static <T> R<T> ok(String msg, T data) {
        return restResult(data, SUCCESS, msg);
    }

    /**
     * 构建失败响应结果
     *
     * @param <T> 响应数据的泛型类型
     * @return 失败响应结果对象
     */
    public static <T> R<T> fail() {
        return restResult(null, ERROR, "操作失败");
    }

    /**
     * 构建失败响应结果（自定义提示信息）
     *
     * @param msg 自定义提示信息
     * @param <T> 响应数据的泛型类型
     * @return 失败响应结果对象
     */
    public static <T> R<T> fail(String msg) {
        return restResult(null, ERROR, msg);
    }

    /**
     * 构建失败响应结果（带业务数据）
     *
     * @param data 业务数据
     * @param <T>  响应数据的泛型类型
     * @return 失败响应结果对象
     */
    public static <T> R<T> fail(T data) {
        return restResult(data, ERROR, "操作失败");
    }

    /**
     * 构建失败响应结果（自定义提示信息+业务数据）
     *
     * @param msg  自定义提示信息
     * @param data 业务数据
     * @param <T>  响应数据的泛型类型
     * @return 失败响应结果对象
     */
    public static <T> R<T> fail(String msg, T data) {
        return restResult(data, ERROR, msg);
    }

    /**
     * 构建失败响应结果（自定义状态码+提示信息）
     *
     * @param code 自定义状态码
     * @param msg  自定义提示信息
     * @param <T>  响应数据的泛型类型
     * @return 失败响应结果对象
     */
    public static <T> R<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 构建警告响应结果
     *
     * @param msg 自定义提示信息
     * @param <T> 响应数据的泛型类型
     * @return 警告响应结果对象
     */
    public static <T> R<T> warn(String msg) {
        return restResult(null, HttpStatus.WARN, msg);
    }

    /**
     * 构建警告响应结果（自定义提示信息+业务数据）
     *
     * @param msg  自定义提示信息
     * @param data 业务数据
     * @param <T>  响应数据的泛型类型
     * @return 警告响应结果对象
     */
    public static <T> R<T> warn(String msg, T data) {
        return restResult(data, HttpStatus.WARN, msg);
    }

    /**
     * 核心构建方法
     *
     * @param data 业务数据
     * @param code 响应状态码
     * @param msg  提示信息
     * @param <T>  响应数据的泛型类型
     * @return 响应结果对象
     */
    private static <T> R<T> restResult(T data, int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }

    /**
     * 判断响应结果是否为失败
     *
     * @param ret 响应结果对象
     * @param <T> 响应数据的泛型类型
     * @return true=失败，false=成功
     */
    public static <T> Boolean isError(R<T> ret) {
        return !isSuccess(ret);
    }

    /**
     * 判断响应结果是否为成功
     *
     * @param ret 响应结果对象
     * @param <T> 响应数据的泛型类型
     * @return true=成功，false=失败
     */
    public static <T> Boolean isSuccess(R<T> ret) {
        return SUCCESS == ret.getCode();
    }

}
