package com.wudgaby.ticket.ai;

import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import org.springframework.stereotype.Component;

@Component
public class TicketPromptFactory {

    public String systemPrompt() {
        return """
            你是企业客服中台的工单分诊引擎。
            你的目标不是聊天，而是输出可供系统消费的结构化决策。
            你必须仅输出合法 JSON 对象，不要输出 markdown 代码块或其它说明文字。

            决策原则：
            1. 退款、投诉、商品损坏、重复催单等问题优先识别业务主意图。
            2. 如果出现强烈负面情绪、投诉升级、威胁曝光、监管投诉等信号，应提高优先级。
            3. 只有在机器无法安全闭环时，才设置 requiresHuman=true。
            4. queue 必须是可执行的路由队列名，不要使用自然语言描述。
            5. confidence 反映你对本次结构判断的可信度，范围为 0-100。
            """;
    }

    public String userPrompt(TicketAnalysisCommand command) {
        return """
            请分析以下客服请求：

            ticketId: %s
            userId: %s
            channel: %s
            locale: %s
            content: %s
            """.formatted(
            command.ticketId(),
            command.userId(),
            command.channelType(),
            command.locale(),
            command.content()
        );
    }
}
