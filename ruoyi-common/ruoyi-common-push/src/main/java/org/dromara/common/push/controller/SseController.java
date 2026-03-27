package org.dromara.common.push.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.push.core.SseEmitterSessionManager;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 控制器
 *
 * @author Lion Li
 */
@RestController
@RequiredArgsConstructor
public class SseController implements DisposableBean {

    private final SseEmitterSessionManager sessionManager;

    /**
     * 建立当前登录用户的 SSE 连接。
     *
     * @return SSE 发射器
     */
    @GetMapping(value = "${message.path:/resource/message}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        return sessionManager.connect(userId, tokenValue);
    }

    /**
     * 关闭当前登录用户的 SSE 连接。
     *
     * @return 操作结果
     */
    @SaIgnore
    @GetMapping(value = "${message.path:/resource/message}/close")
    public R<Void> close() {
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        sessionManager.disconnect(userId, tokenValue);
        return R.ok();
    }

    // 以下为demo仅供参考 禁止使用 请在业务逻辑中使用工具发送而不是用接口发送
//    /**
//     * 向特定用户发送消息
//     *
//     * @param userId 目标用户的 ID
//     * @param msg    要发送的消息内容
//     */
//    @GetMapping(value = "${message.path:/resource/message}/send")
//    public R<Void> send(Long userId, String msg) {
//        PushDTO dto = new PushDTO();
//        dto.setUserIds(List.of(userId));
//        dto.setPayload(PushPayloadDTO.of("message", "backend", msg, null));
//        sessionManager.publishMessage(dto);
//        return R.ok();
//    }
//
//    /**
//     * 向所有用户发送消息
//     *
//     * @param msg 要发送的消息内容
//     */
//    @GetMapping(value = "${message.path:/resource/message}/sendAll")
//    public R<Void> send(String msg) {
//        sessionManager.publishAll(msg);
//        return R.ok();
//    }

    /**
     * 容器销毁时释放资源占位实现。
     *
     * @throws Exception 销毁异常
     */
    @Override
    public void destroy() throws Exception {
        // 销毁时不需要做什么 此方法避免无用操作报错
    }

}
