package org.dromara.snailai;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Snail AI Server 启动程序
 *
 * @author Lion Li
 * @date 2026-05-26
 */
@SpringBootApplication
public class SnailAiServerApplication {

    /**
     * Snail AI 服务启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        com.aizuda.snail.ai.starter.SnailAiSpringbootApplication.main(args);
    }

}
