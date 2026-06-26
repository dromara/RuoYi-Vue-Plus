package org.dromara.ai.controller;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserRegisterRequest;
import com.aizuda.snail.ai.common.openapi.dto.OpenApiUserVO;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.api.model.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Snail AI OpenAPI 控制器
 *
 * @author opensnail
 * @date 2026-04-25
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/snail-ai")
@RequiredArgsConstructor
@ConditionalOnBean(OpenApiUserClient.class)
public class SnailAiController extends BaseController {

    /**
     * Snail AI 成功状态码。
     */
    private static final int SNAIL_AI_SUCCESS = 1;
    private final OpenApiUserClient userClient;

    /**
     * 注册当前登录用户并返回 OpenAPI 用户信息。
     */
    @PostMapping("/user/register")
    public R<OpenApiUserVO> registerCurrentUser() {
        return R.ok(ensureOpenApiUser());
    }

    /**
     * 确保当前登录用户已注册为 OpenAPI 用户。
     */
    private OpenApiUserVO ensureOpenApiUser() {
        Long userId = LoginHelper.getUserId();
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || userId == null) {
            throw new SnailAiException("当前登录用户为空");
        }

        OpenApiUserRegisterRequest registerRequest = new OpenApiUserRegisterRequest();
        registerRequest.setExternalId(String.valueOf(userId));
        registerRequest.setNickname(loginUser.getNickname());
        Result<OpenApiUserVO> registerResult = userClient.register(registerRequest);
        if (registerResult == null) {
            throw new SnailAiException("注册 OpenAPI 用户失败，返回为空");
        }
        if (registerResult.getStatus() != SNAIL_AI_SUCCESS) {
            throw new SnailAiException(registerResult.getMessage());
        }
        if (registerResult.getData() == null) {
            throw new SnailAiException("注册 OpenAPI 用户失败，返回为空");
        }
        return registerResult.getData();
    }
}
