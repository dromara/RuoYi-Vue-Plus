package com.wudgaby.stars.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.wudgaby.stars.config.StarsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubApiClient {

    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final Pattern PAGE_PARAM = Pattern.compile("[?&]page=(\\d+)");
    private static final int MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;

    private final WebClient webClient;
    private final StarsProperties properties;

    public GitHubApiClient(WebClient.Builder webClientBuilder, StarsProperties properties) {
        this.properties = properties;
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codec -> codec.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
            .build();
        this.webClient = webClientBuilder
            .exchangeStrategies(strategies)
            .baseUrl(properties.github().apiBase())
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
    }

    public GitHubUser fetchCurrentUser(String token) {
        return webClient.get()
            .uri("/user")
            .headers(headers -> headers.setBearerAuth(token))
            .exchangeToMono(response -> {
                if (isRateLimited(response.statusCode())) {
                    return Mono.error(new GitHubRateLimitException(parseRetryAfter(response.headers().asHttpHeaders())));
                }
                if (response.statusCode().isError()) {
                    return response.createException().flatMap(Mono::error);
                }
                String scope = response.headers().asHttpHeaders().getFirst("X-OAuth-Scopes");
                return response.bodyToMono(JsonNode.class)
                    .map(body -> new GitHubUser(body.path("login").asText(null), scope));
            })
            .block();
    }

    public GitHubStarredPage fetchUserStarred(String token, String username, int page) {
        String path = username == null ? "/user/starred" : "/users/" + username + "/starred";
        int pageSize = properties.github().pageSize();

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(path)
                .queryParam("per_page", pageSize)
                .queryParam("page", page)
                .queryParam("sort", "created")
                .queryParam("direction", "desc")
                .build())
            .headers(headers -> headers.setBearerAuth(token))
            .exchangeToMono(response -> {
                if (isRateLimited(response.statusCode())) {
                    return Mono.error(new GitHubRateLimitException(parseRetryAfter(response.headers().asHttpHeaders())));
                }
                if (response.statusCode().isError()) {
                    return response.createException().flatMap(Mono::error);
                }
                String linkHeader = response.headers().asHttpHeaders().getFirst(HttpHeaders.LINK);
                return response.bodyToMono(JsonNode.class)
                    .map(body -> toStarredPage(body, linkHeader));
            })
            .block();
    }

    public String fetchReadmeSnippet(String token, String owner, String repo, int maxChars) {
        return webClient.get()
            .uri("/repos/{owner}/{repo}/readme", owner, repo)
            .headers(headers -> headers.setBearerAuth(token))
            .exchangeToMono(response -> {
                if (isRateLimited(response.statusCode())) {
                    return Mono.error(new GitHubRateLimitException(parseRetryAfter(response.headers().asHttpHeaders())));
                }
                if (response.statusCode().value() == 404) {
                    return Mono.just("");
                }
                if (response.statusCode().isError()) {
                    return response.createException().flatMap(Mono::error);
                }
                return response.bodyToMono(JsonNode.class)
                    .map(body -> decodeAndTruncate(body.path("content").asText(""), maxChars));
            })
            .block();
    }

    private GitHubStarredPage toStarredPage(JsonNode body, String linkHeader) {
        List<GitHubStarredRepo> items = new ArrayList<>();
        if (body != null && body.isArray()) {
            for (JsonNode node : body) {
                items.add(toStarredRepo(node));
            }
        }
        int nextPage = parseNextPage(linkHeader);
        return new GitHubStarredPage(items, nextPage > 0, nextPage);
    }

    private GitHubStarredRepo toStarredRepo(JsonNode node) {
        return new GitHubStarredRepo(
            node.path("full_name").asText(null),
            node.path("owner").path("login").asText(null),
            node.path("name").asText(null),
            node.path("description").asText(null),
            node.path("language").asText(null),
            node.path("stargazers_count").asInt(0),
            node.path("html_url").asText(null),
            parseInstant(node.path("updated_at").asText(null))
        );
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }

    private int parseNextPage(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return 0;
        }
        Matcher linkMatcher = NEXT_LINK.matcher(linkHeader);
        if (!linkMatcher.find()) {
            return 0;
        }
        Matcher pageMatcher = PAGE_PARAM.matcher(linkMatcher.group(1));
        if (!pageMatcher.find()) {
            return 0;
        }
        return Integer.parseInt(pageMatcher.group(1));
    }

    private String decodeAndTruncate(String base64Content, int maxChars) {
        if (base64Content == null || base64Content.isBlank()) {
            return "";
        }
        String normalized = base64Content.replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        String text = new String(decoded, StandardCharsets.UTF_8);
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private boolean isRateLimited(HttpStatusCode statusCode) {
        int code = statusCode.value();
        return code == 403 || code == 429;
    }

    private long parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null || retryAfter.isBlank()) {
            return 60L;
        }
        try {
            return Long.parseLong(retryAfter.trim());
        } catch (NumberFormatException ignored) {
            return 60L;
        }
    }
}
