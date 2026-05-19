package com.wudgaby.ticket.services;

import cn.hutool.core.util.StrUtil;
import com.wudgaby.ticket.application.TicketDecisionJsonCodec;
import com.wudgaby.ticket.domain.TicketRoutingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TicketRoutingResultHandler {

    private static final Logger log = LoggerFactory.getLogger(TicketRoutingResultHandler.class);
    private static final String RESULT_KEY_PREFIX = "ai:ticket:routing:result:";
    private static final Duration RESULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final TicketDecisionJsonCodec jsonCodec;

    public TicketRoutingResultHandler(StringRedisTemplate redisTemplate, TicketDecisionJsonCodec jsonCodec) {
        this.redisTemplate = redisTemplate;
        this.jsonCodec = jsonCodec;
    }

    public void handle(TicketRoutingResult result) {
        String key = RESULT_KEY_PREFIX + result.ticketId();
        redisTemplate.opsForValue().set(key, jsonCodec.write(result), RESULT_TTL);

        var decision = result.decision();
        log.info(
            "Applied routing result: ticketId={}, queue={}, intent={}, priority={}, requiresHuman={}, processedAt={}",
            result.ticketId(),
            decision.queue(),
            decision.intent(),
            decision.priority(),
            decision.requiresHuman(),
            result.processedAt());
    }

    public Optional<TicketRoutingResult> findByTicketId(String ticketId) {
        String cached = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + ticketId);
        if (StrUtil.isBlank(cached)) {
            return Optional.empty();
        }
        return Optional.of(jsonCodec.readResult(cached));
    }
}
