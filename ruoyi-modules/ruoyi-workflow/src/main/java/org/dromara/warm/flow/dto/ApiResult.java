/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @author ruoyi
 */
@Setter
@Getter
public class ApiResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 失败
     */
    public static final int FAIL = 500;

    private int code;

    private String msg;

    private T data;

    public static <T> ApiResult<T> ok() {
        return restResult(null, SUCCESS, "操作成功");
    }

    public static <T> ApiResult<T> ok(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    public static <T> ApiResult<T> ok(T data, String msg) {
        return restResult(data, SUCCESS, msg);
    }

    public static <T> ApiResult<T> fail() {
        return restResult(null, FAIL, "操作失败");
    }

    public static <T> ApiResult<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    public static <T> ApiResult<T> fail(T data) {
        return restResult(data, FAIL, "操作失败");
    }

    public static <T> ApiResult<T> fail(T data, String msg) {
        return restResult(data, FAIL, msg);
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    private static <T> ApiResult<T> restResult(T data, int code, String msg) {
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

    public static <T> Boolean isError(ApiResult<T> ret) {
        return !isSuccess(ret);
    }

    public static <T> Boolean isSuccess(ApiResult<T> ret) {
        return ApiResult.SUCCESS == ret.getCode();
    }
}
