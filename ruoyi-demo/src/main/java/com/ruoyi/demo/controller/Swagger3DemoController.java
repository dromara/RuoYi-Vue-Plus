package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * swagger3 用法示例
 *
 * @author Lion Li
 */
@Tag(name ="演示swagger3控制器", description = "演示swagger3接口")
@RestController
@RequestMapping("/swagger/demo")
public class Swagger3DemoController {

    /**
     * 上传请求
     * 必须使用 @RequestPart 注解标注为文件
     */
    @Parameters({
        @Parameter(name = "file", description = "文件", in = ParameterIn.QUERY, required = true)
    })
    @PostMapping(value = "/upload")
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        return R.ok("操作成功", file.getOriginalFilename());
    }

}
