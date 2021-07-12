package com.ruoyi.demo.feign.fallback;


import com.ruoyi.demo.feign.FeignTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * feign测试fallback
 * 自定义封装结构体熔断
 * 需重写解码器 根据自定义实体 自行解析熔断
 *
 * @see {com.ruoyi.framework.config.FeignConfig#errorDecoder()}
 * @author Lion Li
 */
@Slf4j
@Component
public class FeignTestFallback implements FeignTestService {

    @Override
    public String search(String wd) {
        log.error("fallback");
        return "报错啦";
    }
}
