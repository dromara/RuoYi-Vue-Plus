package org.dromara.common.oss.model;

import java.util.List;

/**
 * 批量删除结果；请求成功也可能包含逐项失败。
 *
 * @param deletedKeys 已删除的对象键
 * @param errors 逐项失败信息
 */
public record DeleteObjectsResult(List<String> deletedKeys, List<Error> errors) {

    /**
     * 删除单个对象时的服务端错误。
     *
     * @param key 对象键
     * @param code 错误码
     * @param message 错误信息
     */
    public record Error(String key, String code, String message) {
    }
}
