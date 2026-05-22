package com.wudgaby.stars.service;

import com.wudgaby.stars.messaging.EnrichmentCommand;

/**
 * 仓库 AI enrichment 服务
 */
public interface IStarsEnrichmentService {

    /**
     * 处理 enrichment 命令（Kafka 消费入口）
     */
    void process(EnrichmentCommand command);

    /**
     * 重新生成指定用户仓库的 AI 概述与分类
     */
    void requestRegenerate(Long userId, Long userRepoId);
}
