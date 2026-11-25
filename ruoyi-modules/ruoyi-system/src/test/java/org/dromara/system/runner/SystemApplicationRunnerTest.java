package org.dromara.system.runner;

import org.dromara.system.service.ISysOssConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.*;

class SystemApplicationRunnerTest {
    @Test
    void initializesOssConfigurationOnStartup() throws Exception {
        var configurations = mock(ISysOssConfigService.class);
        new SystemApplicationRunner(configurations).run(new DefaultApplicationArguments());
        verify(configurations).init();
    }
}
