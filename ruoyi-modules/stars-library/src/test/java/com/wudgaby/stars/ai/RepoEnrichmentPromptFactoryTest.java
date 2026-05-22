package com.wudgaby.stars.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wudgaby.stars.domain.StarsRepo;
import com.wudgaby.stars.domain.ai.RepoEnrichmentResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class RepoEnrichmentPromptFactoryTest {

    private final RepoEnrichmentPromptFactory promptFactory = new RepoEnrichmentPromptFactory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void systemPromptContainsCategoryTaxonomy() {
        String prompt = promptFactory.systemPrompt();

        assertThat(prompt).contains("AI/RAG");
        assertThat(prompt).contains("后端框架");
        assertThat(prompt).contains("待评估");
    }

    @Test
    void userPromptIncludesRepositoryMetadata() {
        StarsRepo repo = new StarsRepo();
        repo.setFullName("owner/demo");
        repo.setDescription("Demo repository");
        repo.setLanguage("Java");
        repo.setStargazersCount(128);

        String prompt = promptFactory.userPrompt(repo, "# README");

        assertThat(prompt).contains("owner/demo");
        assertThat(prompt).contains("Demo repository");
        assertThat(prompt).contains("Java");
        assertThat(prompt).contains("# README");
        assertThat(prompt).contains("\"one_liner\"");
    }

    @Test
    void parsesSampleEnrichmentJson() throws Exception {
        String json = """
            {
              "one_liner": "面向 RAG 的向量检索框架",
              "summary": "提供文档切分、向量索引与检索能力，适合构建知识库问答系统。",
              "category": "AI/RAG",
              "tags": ["RAG", "向量检索", "知识库"]
            }
            """;

        RepoEnrichmentResult result = objectMapper.readValue(json, RepoEnrichmentResult.class);

        assertThat(result.oneLiner()).isEqualTo("面向 RAG 的向量检索框架");
        assertThat(result.summary()).contains("知识库");
        assertThat(result.category()).isEqualTo("AI/RAG");
        assertThat(result.tags()).containsExactly("RAG", "向量检索", "知识库");
    }
}
