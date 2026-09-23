package org.dromara.workflow.de.cx.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.cx.domain.bo.DeCxValidateBo;
import org.dromara.workflow.de.cx.domain.vo.DeCxValidateVo;
import org.dromara.workflow.de.cx.service.IDeCxService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 上下文构建与消费 CX 域 REST 端点（29 号 §五 CX-10，共 1 端点）。
 *
 * <p>与 {@code DeP1GapCoverController} 的 {@code /de/bp/cx-*} 占位端点的分工：
 * 那些是 P1 档 D 的<b>声明式占位</b>（返回 {@code DEFERRED_TO_OWNER_DECISION}）；
 * 本端点是 CX-10 的<b>真实现</b>（真查库、真判因）。
 *
 * <p><b>「当前用户」从会话取</b>（{@link LoginHelper}），<b>不从请求体取</b> ——
 * 请求体能自报 userId 的话，「当前授权」就等于让调用方自己声明授权。
 *
 * @author AIP
 * @since 2026-09-23
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/cx")
public class DeCxController extends BaseController {

    private final IDeCxService deCxService;

    /**
     * CX-10 上下文使用 / 提交时校验（授权 + 版本 + 有效期）。
     *
     * <p>{@code POST /de/cx/context-validate}，body = {@code {instanceId, action}}。
     * 返回 {@code {valid, reason, currentVersion, minVersion, ...}}；
     * {@code valid=false} 时 {@code reason} 是唯一拒因。
     */
    @SaCheckPermission("workflow:de:cx:validate")
    @PostMapping("/context-validate")
    public R<DeCxValidateVo> contextValidate(@Valid @RequestBody DeCxValidateBo bo) {
        return R.ok(deCxService.contextValidate(
            bo.getInstanceId(), bo.getAction(), LoginHelper.getUserId(), LoginHelper.getUsername()));
    }
}
