package com.wudgaby.stars.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stars")
public record StarsProperties(
    Github github,
    Import importConfig,
    Summary summary,
    DeepLink deepLink
) {

    public record Import(
        /** 未指定 limit 时的默认导入条数（最近 starred） */
        int defaultLimit,
        /** API 允许的单次导入上限 */
        int maxLimit
    ) {
        public Import {
            if (defaultLimit <= 0) {
                defaultLimit = 100;
            }
            if (maxLimit <= 0) {
                maxLimit = 5000;
            }
            if (maxLimit < defaultLimit) {
                maxLimit = defaultLimit;
            }
        }
    }

    public record Github(
        String apiBase,
        int pageSize,
        int requestIntervalMs,
        String fallbackToken,
        String tokenEncryptKey
    ) {
    }

    public record Summary(
        Kafka kafka,
        /** 单 Pod Kafka 监听并发 / 本机 enrichment 槽位 */
        int maxConcurrentPerPod,
        /** 全集群 enrichment 并发上限（Redis 信号量） */
        int maxConcurrentGlobal,
        int maxConcurrentPerUser,
        int readmeMaxChars,
        int retryMax
    ) {

        public Summary {
            if (maxConcurrentPerPod <= 0) {
                maxConcurrentPerPod = 5;
            }
            if (maxConcurrentGlobal <= 0) {
                maxConcurrentGlobal = 30;
            }
            if (maxConcurrentPerUser <= 0) {
                maxConcurrentPerUser = 3;
            }
        }

        public record Kafka(
            String requestTopic,
            String consumerGroup,
            /** 单次 poll 拉取条数（与 BATCH ack 配合，每批处理完再提交 offset）；未配置或 ≤0 时默认 10 */
            int maxPollRecords
        ) {
            public Kafka {
                if (maxPollRecords <= 0) {
                    maxPollRecords = 10;
                }
            }
        }
    }

    public record DeepLink(
        String zreadTemplate,
        String deepwikiTemplate
    ) {
    }
}
