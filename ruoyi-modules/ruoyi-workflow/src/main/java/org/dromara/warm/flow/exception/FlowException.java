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
package org.dromara.warm.flow.exception;
import java.io.Serial;

import lombok.Getter;

/**
 * 流程异常
 *
 * @author warm
 */
public final class FlowException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    @Getter
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     */
    @Getter
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public FlowException() {
    }

    public FlowException(String message) {
        this.message = message;
    }

    public FlowException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public FlowException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public FlowException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public FlowException setMessage(String message) {
        this.message = message;
        return this;
    }

}
