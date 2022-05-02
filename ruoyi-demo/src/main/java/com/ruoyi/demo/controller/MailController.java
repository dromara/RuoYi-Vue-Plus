package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.email.MailUtils;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;


/**
 * 测试邮件发送 Controller
 *
 * @author Michelle.Chung
 */
@Validated
@Api(value = "邮件控制器", tags = {"测试邮件发送"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RequestMapping("/demo/mail")
@RestController
public class MailController {

    /**
     * 发送邮件
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    @GetMapping("/sendSimpleMessage")
    public R<Void> sendSimpleMessage(String to, String subject, String text) {
        MailUtils.send(to, subject, text, false);
        return R.ok("操作成功");
    }

    /**
     * 发送邮件（带附件）
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    @GetMapping("/sendMessageWithAttachment")
    public R<Void> sendMessageWithAttachment(String to, String subject, String text, String filePath) {
        MailUtils.send(to, subject, text, false, new File(filePath));
        return R.ok("操作成功");
    }

}
