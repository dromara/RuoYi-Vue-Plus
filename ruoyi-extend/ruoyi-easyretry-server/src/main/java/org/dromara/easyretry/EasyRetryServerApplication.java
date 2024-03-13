package org.dromara.easyretry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EasyRetry Server 启动程序
 *
 * @author dhb52
 */
@SpringBootApplication
public class EasyRetryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.aizuda.easy.retry.server.EasyRetryServerApplication.class, args);
    }

}
