package org.dromara.powerjob.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import tech.powerjob.server.PowerJobServerApplication;
import tech.powerjob.server.common.utils.PropertyUtils;

/**
 * Admin 监控启动程序
 *
 * @author yhan219
 */
@Slf4j
@EnableScheduling
@SpringBootApplication(scanBasePackages = "tech.powerjob.server")
public class RuoyiPowerJobServerApplication {

    private static final String TIPS = "\n\n" +
        "******************* PowerJob Tips *******************\n" +
        "如果应用无法启动，我们建议您仔细阅读以下文档来解决:\n" +
        "if server can't startup, we recommend that you read the documentation to find a solution:\n" +
        "https://www.yuque.com/powerjob/guidence/problem\n" +
        "******************* PowerJob Tips *******************\n\n";

    public static void main(String[] args) {

        pre();

        // Start SpringBoot application.
        try {
            SpringApplication.run(PowerJobServerApplication.class, args);
        } catch (Throwable t) {
            log.error(TIPS);
            throw t;
        }
    }

    private static void pre() {
        log.info(TIPS);
        PropertyUtils.init();
    }

}
