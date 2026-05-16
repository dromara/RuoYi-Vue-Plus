package org.dromara.demo.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mail.core.MailBuilder;
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
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mail")
public class MailSendController {

    /**
     * 发送邮件
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    @GetMapping("/sendSimpleMessage")
    public R<Void> sendSimpleMessage(String to, String subject, String text) {
        MailBuilder.of().to(to).subject(subject).text(text).send();
        return R.ok();
    }

    /**
     * 发送邮件（带附件）
     *
     * @param to       接收人
     * @param subject  标题
     * @param text     内容
     */
    @GetMapping("/sendMessageWithAttachment")
    public R<Void> sendMessageWithAttachment(String to, String subject, String text) {
        // 附件路径 禁止前端传递 有任意读取系统文件风险
        MailBuilder.of().to(to).subject(subject).text(text).files(new File("/xxx/xxx")).send();
        return R.ok();
    }

    /**
     * 发送邮件（多附件）
     *
     * @param to       接收人
     * @param subject  标题
     * @param text     内容
     */
    @GetMapping("/sendMessageWithAttachments")
    public R<Void> sendMessageWithAttachments(String to, String subject, String text) {
        // 附件路径 禁止前端传递 有任意读取系统文件风险
        String[] paths = new String[]{"/xxx/xxx", "/xxx/xxx"};
        File[] array = Arrays.stream(paths).map(File::new).toArray(File[]::new);
        MailBuilder.of().to(to).subject(subject).text(text).files(array).send();
        return R.ok();
    }

}
