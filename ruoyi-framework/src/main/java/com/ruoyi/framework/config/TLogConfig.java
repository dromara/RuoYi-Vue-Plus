package com.ruoyi.framework.config;

import com.yomahub.tlog.core.aop.AspectLogAop;
import com.yomahub.tlog.spring.TLogPropertyInit;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.springboot.property.TLogProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * 整合 TLog 框架配置
 *
 * @author Lion Li
 * @since 3.3.0
 */
@Order(-999)
@Configuration
@Import(TLogProperty.class)
public class TLogConfig {

    @Bean
    public TLogPropertyInit tLogPropertyInit(TLogProperty tLogProperty) {
        TLogPropertyInit tLogPropertyInit = new TLogPropertyInit();
        tLogPropertyInit.setPattern(tLogProperty.getPattern());
        tLogPropertyInit.setEnableInvokeTimePrint(tLogProperty.enableInvokeTimePrint());
        tLogPropertyInit.setIdGenerator(tLogProperty.getIdGenerator());
        tLogPropertyInit.setMdcEnable(tLogProperty.getMdcEnable());
        return tLogPropertyInit;
    }

    @Bean
    public TLogSpringAware tLogSpringAware(){
        return new TLogSpringAware();
    }

    @Bean
    public AspectLogAop aspectLogAop() {
        return new AspectLogAop();
    }

}
