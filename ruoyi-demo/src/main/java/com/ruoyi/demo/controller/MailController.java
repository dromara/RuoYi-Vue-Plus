package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.email.MailUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;


/**
 * 邮件发送案例
 *
 * @author Michelle.Chung
 */
@Validated
@Api(value = "邮件发送案例", tags = {"邮件发送案例"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mail")
public class MailController {

    @ApiOperation("发送邮件")
    @GetMapping("/sendSimpleMessage")
    public R<Void> sendSimpleMessage(@ApiParam("接收人") String to,
                                     @ApiParam("标题") String subject,
                                     @ApiParam("内容") String text) {
        MailUtils.sendText(to, subject, text);
        return R.ok();
    }

    @ApiOperation("发送邮件（带附件）")
    @GetMapping("/sendMessageWithAttachment")
    public R<Void> sendMessageWithAttachment(@ApiParam("接收人") String to,
                                             @ApiParam("标题") String subject,
                                             @ApiParam("内容") String text,
                                             @ApiParam("附件路径") String filePath) {
        MailUtils.sendText(to, subject, text, new File(filePath));
        return R.ok();
    }

}
