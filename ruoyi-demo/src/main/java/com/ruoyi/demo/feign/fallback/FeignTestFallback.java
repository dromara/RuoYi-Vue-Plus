package com.ruoyi.demo.feign.fallback;


import com.ruoyi.demo.feign.FeignTestService;

public class FeignTestFallback implements FeignTestService {

    @Override
    public String search(String wd) {
        return null;
    }
}
