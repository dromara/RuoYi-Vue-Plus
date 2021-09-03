package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.demo.feign.FeignTestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * feign测试controller
 *
 * @author Lion Li
 */
@Api(value = "feign测试", tags = {"feign测试"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/feign/test")
public class FeignTestController {

    private final FeignTestService feignTestService;

    /**
     * 搜索数据
     */
    @ApiOperation("测试使用feign请求数据")
    @GetMapping("/search/{wd}")
    public AjaxResult search(@PathVariable String wd) {
        String search = feignTestService.search(wd);
        return AjaxResult.success("操作成功",search);
    }
}
