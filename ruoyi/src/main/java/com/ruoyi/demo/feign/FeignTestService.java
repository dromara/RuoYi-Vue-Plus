package com.ruoyi.demo.feign;

import com.ruoyi.demo.feign.fallback.FeignTestFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "baidu",url = "http://www.baidu.com",fallback = FeignTestFallback.class)
public interface FeignTestService {

    @GetMapping("/s")
    String search(@RequestParam("wd") String wd);
}
