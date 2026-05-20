package com.wudgaby.ticket.api.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI Chat Completions 流式 chunk（{@code object=chat.completion.chunk}）。
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/streaming">Chat streaming</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatCompletionChunk(
    String id,
    String object,
    long created,
    String model,
    List<Choice> choices
) {

    public static final String OBJECT_TYPE = "chat.completion.chunk";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Choice(
        int index,
        Delta delta,
        @JsonProperty("finish_reason") String finishReason
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Delta(String role, String content) {
    }
}
