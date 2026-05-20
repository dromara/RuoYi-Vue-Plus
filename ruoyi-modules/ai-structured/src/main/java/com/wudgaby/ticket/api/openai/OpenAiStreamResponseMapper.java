package com.wudgaby.ticket.api.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 将文本流包装为 OpenAI 兼容的 SSE（{@code data: {...}} / {@code data: [DONE]}）。
 */
@Component
public class OpenAiStreamResponseMapper {

    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiStreamResponseMapper(
        ObjectMapper objectMapper,
        @Value("${spring.ai.deepseek.chat.options.model:deepseek-chat}") String model) {
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public Flux<ServerSentEvent<String>> toSse(Flux<String> contentChunks) {
        String id = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        long created = Instant.now().getEpochSecond();

        Flux<ServerSentEvent<String>> roleChunk = Flux.just(
            chunkEvent(id, created, new OpenAiChatCompletionChunk.Delta("assistant", null), null)
        );

        Flux<ServerSentEvent<String>> contentEvents = contentChunks
            .map(content -> chunkEvent(id, created, new OpenAiChatCompletionChunk.Delta(null, content), null));

        Flux<ServerSentEvent<String>> tail = Flux.just(
            chunkEvent(id, created, new OpenAiChatCompletionChunk.Delta(null, null), "stop"),
            ServerSentEvent.<String>builder().data("[DONE]").build()
        );

        return Flux.concat(roleChunk, contentEvents, tail);
    }

    private ServerSentEvent<String> chunkEvent(
        String id,
        long created,
        OpenAiChatCompletionChunk.Delta delta,
        String finishReason) {
        OpenAiChatCompletionChunk chunk = new OpenAiChatCompletionChunk(
            id,
            OpenAiChatCompletionChunk.OBJECT_TYPE,
            created,
            model,
            List.of(new OpenAiChatCompletionChunk.Choice(0, delta, finishReason))
        );
        return ServerSentEvent.<String>builder().data(toJson(chunk)).build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OpenAI chunk", ex);
        }
    }
}
