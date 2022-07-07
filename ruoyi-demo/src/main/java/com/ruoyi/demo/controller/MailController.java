package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.email.MailUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name ="邮件发送案例", description = "邮件发送案例")
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mail")
public class MailController {

    @GetMapping("/sendSimpleMessage")
    public R<Void> sendSimpleMessage(@Parameter(name = "接收人") String to,
                                     @Parameter(name = "标题") String subject,
                                     @Parameter(name = "内容") String text) {
        MailUtils.sendText(to, subject, text);
        return R.ok();
    }

    @GetMapping("/sendMessageWithAttachment")
    public R<Void> sendMessageWithAttachment(@Parameter(name = "接收人") String to,
                                             @Parameter(name = "标题") String subject,
                                             @Parameter(name = "内容") String text,
                                             @Parameter(name = "附件路径") String filePath) {
        MailUtils.sendText(to, subject, text, new File(filePath));
        return R.ok();
    }

}
