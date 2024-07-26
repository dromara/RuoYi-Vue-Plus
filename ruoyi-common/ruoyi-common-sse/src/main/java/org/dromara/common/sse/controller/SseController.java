package org.dromara.common.sse.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.sse.core.SseEmitterManager;
import org.dromara.common.sse.dto.SseMessageDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "${sse.path}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        return sseEmitterManager.connect(userId, tokenValue);
    }

    @GetMapping(value = "${sse.path}/close")
    public R<Void> close() {
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        sseEmitterManager.disconnect(userId, tokenValue);
        return R.ok();
    }

    @GetMapping(value = "${sse.path}/send")
    public R<Void> send(Long userId, String msg) {
        SseMessageDto dto = new SseMessageDto();
        dto.setUserIds(List.of(userId));
        dto.setMessage(msg);
        sseEmitterManager.publishMessage(dto);
        return R.ok();
    }

    @GetMapping(value = "${sse.path}/sendAll")
    public R<Void> send(String msg) {
        sseEmitterManager.publishAll(msg);
        return R.ok();
    }

}
