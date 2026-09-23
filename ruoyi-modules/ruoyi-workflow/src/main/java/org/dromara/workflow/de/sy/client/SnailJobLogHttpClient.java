package org.dromara.workflow.de.sy.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.Map;

/**
 * SnailJob 调度日志 HTTP 桥接客户端（SY-03 · 2026-09-23）。
 *
 * <p><b>为什么需要 HTTP 桥接</b>：sj_job_log_message 在独立微服务
 * {@code ruoyi-snailjob-server}（{@code 04-backend-code/ruoyi-extend/ruoyi-snailjob-server}）
 * 的库中，与本仓库分离。ruoyi-workflow 不能直接读对方的表（既无直接依赖，也
 * 无 DB 跨库视图），故走 HTTP 桥接。
 *
 * <p><b>连接配置</b>（从 {@code application.yml} 读）：
 * <ul>
 *   <li>{@code aip.snailjob.base-url} —— 默认 {@code http://localhost:18082}
 *       （ruoyi-snailjob-server 默认端口，见
 *       {@code 04-backend-code/ruoyi-extend/ruoyi-snailjob-server/src/main/resources/application.yml}）</li>
 *   <li>{@code aip.snailjob.job-log-path} —— 默认 {@code /job-log/list}</li>
 * </ul>
 *
 * <p><b>降级语义</b>：本类若 SnailJob 不可达（连接超时 / 404），<b>不抛异常</b>
 * —— 返回空 {@code Map} + log warn。这是「探针级联 fail-soft」（P-3 政策）的
 * 一致语义：snailjob 调度日志作为辅佐视图，不可达不影响主流程。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Component
public class SnailJobLogHttpClient {

    @Value("${aip.snailjob.base-url:http://localhost:18082}")
    private String baseUrl;

    @Value("${aip.snailjob.job-log-path:/job-log/list}")
    private String jobLogPath;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * 查询 SnailJob 调度日志（fail-soft）。
     *
     * @param query 过滤条件（groupName / jobId / taskBatchId 任选）
     * @param pageNum 页码（从 1 起）
     * @param pageSize 页大小
     * @return SnailJob 响应（data + total）。不可达 ⇒ 空 Map。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listJobLog(Map<String, Object> query, int pageNum, int pageSize) {
        String url = baseUrl + jobLogPath;
        try {
            Map<String, Object> body = query == null ? Collections.emptyMap() : query;
            body.putIfAbsent("pageNum", pageNum);
            body.putIfAbsent("pageSize", pageSize);
            Map<String, Object> resp = restClient.post()
                .uri(url)
                .body(body)
                .retrieve()
                .body(Map.class);
            return resp == null ? Collections.emptyMap() : resp;
        } catch (Exception ex) {
            // fail-soft：snailjob 不可达不影响主流程
            log.warn("[snailjob.log.list] 调用失败 url={} msg={} —— 返回空响应（fail-soft）",
                url, ex.getMessage());
            return Collections.emptyMap();
        }
    }
}