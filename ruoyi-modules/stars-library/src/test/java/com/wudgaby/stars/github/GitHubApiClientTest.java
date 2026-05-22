package com.wudgaby.stars.github;

import com.wudgaby.stars.config.StarsProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class GitHubApiClientTest {

    private MockWebServer mockWebServer;
    private GitHubApiClient client;
    private StarsProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        properties = testProperties(mockWebServer.url("/").toString());
        client = new GitHubApiClient(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void fetchUserStarred_parsesItemsAndNextLink() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .addHeader("Link", "<" + mockWebServer.url("/user/starred?page=2&per_page=100") + ">; rel=\"next\"")
            .setBody("""
                [
                  {
                    "full_name": "octocat/Hello-World",
                    "name": "Hello-World",
                    "owner": { "login": "octocat" },
                    "description": "My first repo",
                    "language": "Java",
                    "stargazers_count": 80,
                    "html_url": "https://github.com/octocat/Hello-World",
                    "updated_at": "2024-01-15T10:00:00Z"
                  }
                ]
                """));

        GitHubStarredPage page = client.fetchUserStarred("ghp_test", null, 1);

        assertThat(page.items()).hasSize(1);
        GitHubStarredRepo repo = page.items().get(0);
        assertThat(repo.fullName()).isEqualTo("octocat/Hello-World");
        assertThat(repo.owner()).isEqualTo("octocat");
        assertThat(repo.name()).isEqualTo("Hello-World");
        assertThat(repo.description()).isEqualTo("My first repo");
        assertThat(repo.language()).isEqualTo("Java");
        assertThat(repo.stargazersCount()).isEqualTo(80);
        assertThat(repo.htmlUrl()).isEqualTo("https://github.com/octocat/Hello-World");
        assertThat(repo.updatedAt()).isEqualTo(Instant.parse("2024-01-15T10:00:00Z"));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextPage()).isEqualTo(2);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/user/starred?per_page=100&page=1");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer ghp_test");
    }

    @Test
    void fetchUserStarred_usesUsersPathWhenUsernameProvided() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("[]"));

        GitHubStarredPage page = client.fetchUserStarred("ghp_test", "torvalds", 3);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextPage()).isZero();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/users/torvalds/starred?per_page=100&page=3");
    }

    @Test
    void fetchReadmeSnippet_decodesBase64AndTruncates() throws InterruptedException {
        String readme = "A".repeat(20);
        String encoded = Base64.getEncoder().encodeToString(readme.getBytes(StandardCharsets.UTF_8));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""
                {
                  "content": "%s",
                  "encoding": "base64"
                }
                """.formatted(encoded)));

        String snippet = client.fetchReadmeSnippet("ghp_test", "octocat", "Hello-World", 10);

        assertThat(snippet).isEqualTo("A".repeat(10));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/repos/octocat/Hello-World/readme");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer ghp_test");
    }

    @Test
    void fetchUserStarred_throwsRateLimitExceptionOn403() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(403)
            .addHeader("Retry-After", "120"));

        assertThatThrownBy(() -> client.fetchUserStarred("ghp_test", null, 1))
            .isInstanceOf(GitHubRateLimitException.class)
            .extracting("retryAfterSeconds")
            .isEqualTo(120L);
    }

    @Test
    void fetchUserStarred_throwsRateLimitExceptionOn429() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .addHeader("Retry-After", "60"));

        assertThatThrownBy(() -> client.fetchUserStarred("ghp_test", null, 1))
            .isInstanceOf(GitHubRateLimitException.class)
            .extracting("retryAfterSeconds")
            .isEqualTo(60L);
    }

    private static StarsProperties testProperties(String apiBase) {
        return new StarsProperties(
            new StarsProperties.Github(apiBase, 100, 100, null, null),
            new StarsProperties.Import(100, 5000),
            new StarsProperties.Summary(
                new StarsProperties.Summary.Kafka("stars.enrichment.request", "stars-enrichment-consumer", 10),
                5,
                30,
                3,
                3000,
                3
            ),
            new StarsProperties.DeepLink(
                "https://zread.ai/{owner}/{repo}",
                "https://deepwiki.com/{owner}/{repo}"
            )
        );
    }
}
