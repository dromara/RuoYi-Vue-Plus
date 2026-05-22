package com.wudgaby.stars.ai;

import com.wudgaby.stars.domain.StarsRepo;
import org.springframework.stereotype.Component;

/**
 * 仓库 enrichment 中文 Prompt 工厂
 */
@Component
public class RepoEnrichmentPromptFactory {

    public static final String CATEGORY_TAXONOMY =
        "AI/RAG, 后端框架, 前端组件, DevOps, 数据库, 工具库, 学习参考, 待评估";

    public String systemPrompt() {
        return """
            你是 GitHub 技术项目分析助手。
            你的任务是根据仓库元数据与 README 摘要，输出可供系统直接消费的结构化 JSON。
            你必须仅输出合法 JSON 对象，不要输出 markdown 代码块或其它说明文字。

            JSON 字段：
            - one_liner：中文一句话概述，不超过 50 字
            - summary：中文概述，不超过 200 字，最多 3 行语义
            - category：主分类，优先从 taxonomy 中选择；若无合适项可给出最接近的中文分类
            - tags：最多 5 个中文标签，用于检索与归档

            category taxonomy（优先选择）：
            %s
            """.formatted(CATEGORY_TAXONOMY);
    }

    public String userPrompt(StarsRepo repo, String readmeSnippet) {
        return """
            请分析以下 GitHub 仓库并输出 JSON：
            {"one_liner":"...","summary":"...","category":"...","tags":["..."]}

            仓库：%s
            Description：%s
            Language：%s
            Stars：%s
            README 摘要：
            %s
            """.formatted(
            nullToEmpty(repo.getFullName()),
            nullToEmpty(repo.getDescription()),
            nullToEmpty(repo.getLanguage()),
            repo.getStargazersCount() == null ? "0" : repo.getStargazersCount(),
            nullToEmpty(readmeSnippet)
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
