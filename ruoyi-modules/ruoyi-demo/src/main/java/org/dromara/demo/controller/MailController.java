package org.dromara.demo.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mail.utils.MailUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Arrays;


/**
 * 邮件发送案例
 *
 * @author Michelle.Chung
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mail")
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
        MailUtils.sendText(to, subject, text);
        return R.ok();
    }

    /**
     * 发送邮件（带附件）
     *
     * @param to       接收人
     * @param subject  标题
     * @param text     内容
     * @param filePath 附件路径
     */
    @GetMapping("/sendMessageWithAttachment")
    public R<Void> sendMessageWithAttachment(String to, String subject, String text, String filePath) {
        MailUtils.sendText(to, subject, text, new File(filePath));
        return R.ok();
    }

    /**
     * 发送邮件（多附件）
     *
     * @param to       接收人
     * @param subject  标题
     * @param text     内容
     * @param paths    附件路径
     */
    @GetMapping("/sendMessageWithAttachments")
    public R<Void> sendMessageWithAttachments(String to, String subject, String text, String[] paths) {
        File[] array = Arrays.stream(paths).map(File::new).toArray(File[]::new);
        MailUtils.sendText(to, subject, text, array);
        return R.ok();
    }

}
