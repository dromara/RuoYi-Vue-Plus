package org.dromara.monitor.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin 监控启动程序
 *
 * @author Lion Li
 */
@SpringBootApplication
public class MonitorAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorAdminApplication.class, args);
        System.out.println("Admin 监控启动成功");
    }

}
