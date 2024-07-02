package org.dromara.common.web.config;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * Undertow 自定义配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class UndertowConfig implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(true, 1024));
            deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo);
            // 使用虚拟线程
            if (SpringUtils.isVirtual()) {
                VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor("undertow-");
                deploymentInfo.setExecutor(executor);
                deploymentInfo.setAsyncExecutor(executor);
            }
        });
    }

}
