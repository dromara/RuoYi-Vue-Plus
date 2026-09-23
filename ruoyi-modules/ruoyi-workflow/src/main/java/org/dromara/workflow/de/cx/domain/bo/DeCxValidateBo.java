package org.dromara.workflow.de.cx.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * CX-10 上下文校验请求 BO。
 *
 * <p>29 号 §五 CX-10：「上下文<b>使用和提交</b>时校验当前授权、版本和有效期」
 * ⇒ {@code action} 就是这两个时点的区分位（{@code read} = 使用 / {@code write} = 提交）。
 *
 * <p><b>刻意不接收 {@code minVersion}</b>：CX-10 验收「最低版本来自可信服务端事实」
 * —— 若允许调用方传最低版本，调用方就能把门槛调到自己的版本上，这条验收即落空。
 * 服务端最低版本只从服务端常量取（见 {@code DeCxServiceImpl.SERVER_MIN_VERSION}）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeCxValidateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 目标实例 ID（flow_instance.id，必填） */
    @NotNull
    private Long instanceId;

    /** 校验时点：read（上下文使用）/ write（上下文提交）；空 = read */
    private String action;
}
