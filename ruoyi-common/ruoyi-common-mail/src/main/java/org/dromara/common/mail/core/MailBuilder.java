package org.dromara.common.mail.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.JakartaMail;
import cn.hutool.extra.mail.MailAccount;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * 邮件发送构建器。
 */
public final class MailBuilder {

    private MailAccount mailAccount;

    private boolean useGlobalSession = true;

    private final List<String> tos = new ArrayList<>();

    private final List<String> ccs = new ArrayList<>();

    private final List<String> bccs = new ArrayList<>();

    private final Map<String, InputStream> images = new LinkedHashMap<>();

    private File[] files = new File[0];

    private String from;

    private String user;

    private String pass;

    private String subject;

    private String content;

    private boolean html;

    private MailBuilder() {
    }

    /**
     * 创建邮件构建器，默认使用配置文件中的邮件账户。
     *
     * @return 邮件构建器
     */
    public static MailBuilder of() {
        return new MailBuilder();
    }

    /**
     * 创建邮件构建器，使用指定邮件账户。
     *
     * @param mailAccount 邮件账户
     * @return 邮件构建器
     */
    public static MailBuilder of(MailAccount mailAccount) {
        return new MailBuilder().account(mailAccount);
    }

    /**
     * 设置自定义邮件账户。
     *
     * @param mailAccount 邮件账户
     * @return 当前构建器
     */
    public MailBuilder account(MailAccount mailAccount) {
        this.mailAccount = Objects.requireNonNull(mailAccount, "mailAccount must not be null");
        this.useGlobalSession = false;
        return this;
    }

    /**
     * 覆盖发件人。
     *
     * @param from 发件人
     * @return 当前构建器
     */
    public MailBuilder from(String from) {
        this.from = from;
        this.useGlobalSession = false;
        return this;
    }

    /**
     * 覆盖登录用户名。
     *
     * @param user 用户名
     * @return 当前构建器
     */
    public MailBuilder user(String user) {
        this.user = user;
        this.useGlobalSession = false;
        return this;
    }

    /**
     * 覆盖登录密码或授权码。
     *
     * @param pass 密码或授权码
     * @return 当前构建器
     */
    public MailBuilder pass(String pass) {
        this.pass = pass;
        this.useGlobalSession = false;
        return this;
    }

    /**
     * 添加收件人，多个邮箱可使用逗号或分号分隔。
     *
     * @param addresses 收件人
     * @return 当前构建器
     */
    public MailBuilder to(String addresses) {
        this.tos.addAll(splitAddress(addresses));
        return this;
    }

    /**
     * 添加收件人。
     *
     * @param addresses 收件人集合
     * @return 当前构建器
     */
    public MailBuilder to(Collection<String> addresses) {
        this.tos.addAll(normalizeAddresses(addresses));
        return this;
    }

    /**
     * 添加抄送人，多个邮箱可使用逗号或分号分隔。
     *
     * @param addresses 抄送人
     * @return 当前构建器
     */
    public MailBuilder cc(String addresses) {
        this.ccs.addAll(splitAddress(addresses));
        return this;
    }

    /**
     * 添加抄送人。
     *
     * @param addresses 抄送人集合
     * @return 当前构建器
     */
    public MailBuilder cc(Collection<String> addresses) {
        this.ccs.addAll(normalizeAddresses(addresses));
        return this;
    }

    /**
     * 添加密送人，多个邮箱可使用逗号或分号分隔。
     *
     * @param addresses 密送人
     * @return 当前构建器
     */
    public MailBuilder bcc(String addresses) {
        this.bccs.addAll(splitAddress(addresses));
        return this;
    }

    /**
     * 添加密送人。
     *
     * @param addresses 密送人集合
     * @return 当前构建器
     */
    public MailBuilder bcc(Collection<String> addresses) {
        this.bccs.addAll(normalizeAddresses(addresses));
        return this;
    }

    /**
     * 设置邮件标题。
     *
     * @param subject 标题
     * @return 当前构建器
     */
    public MailBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * 设置文本正文。
     *
     * @param content 正文
     * @return 当前构建器
     */
    public MailBuilder text(String content) {
        this.content = content;
        this.html = false;
        return this;
    }

    /**
     * 设置 HTML 正文。
     *
     * @param content 正文
     * @return 当前构建器
     */
    public MailBuilder html(String content) {
        this.content = content;
        this.html = true;
        return this;
    }

    /**
     * 设置正文。
     *
     * @param content 正文
     * @param html 是否 HTML
     * @return 当前构建器
     */
    public MailBuilder content(String content, boolean html) {
        this.content = content;
        this.html = html;
        return this;
    }

    /**
     * 添加内联图片。
     *
     * @param cid 图片 cid
     * @param inputStream 图片输入流
     * @return 当前构建器
     */
    public MailBuilder image(String cid, InputStream inputStream) {
        if (StrUtil.isNotBlank(cid) && inputStream != null) {
            this.images.put(cid, inputStream);
        }
        return this;
    }

    /**
     * 添加内联图片。
     *
     * @param imageMap 图片 cid 与输入流映射
     * @return 当前构建器
     */
    public MailBuilder images(Map<String, InputStream> imageMap) {
        if (MapUtil.isNotEmpty(imageMap)) {
            imageMap.forEach(this::image);
        }
        return this;
    }

    /**
     * 设置附件。
     *
     * @param files 附件列表
     * @return 当前构建器
     */
    public MailBuilder files(File... files) {
        this.files = files == null ? new File[0] : files;
        return this;
    }

    /**
     * 发送邮件。
     *
     * @return message-id
     */
    public String send() {
        validate();
        MailAccount account = resolveMailAccount();
        JakartaMail mail = JakartaMail.create(account).setUseGlobalSession(useGlobalSession);
        mail.setTos(tos.toArray(new String[0]));
        if (CollUtil.isNotEmpty(ccs)) {
            mail.setCcs(ccs.toArray(new String[0]));
        }
        if (CollUtil.isNotEmpty(bccs)) {
            mail.setBccs(bccs.toArray(new String[0]));
        }
        mail.setTitle(subject);
        mail.setContent(content);
        mail.setHtml(html);
        mail.setFiles(files);
        try {
            if (MapUtil.isNotEmpty(images)) {
                images.forEach(mail::addImage);
            }
            return mail.send();
        } finally {
            images.values().forEach(IoUtil::close);
        }
    }

    private void validate() {
        if (CollUtil.isEmpty(tos)) {
            throw new IllegalArgumentException("邮件收件人不能为空");
        }
        if (StrUtil.isBlank(subject)) {
            throw new IllegalArgumentException("邮件标题不能为空");
        }
        if (content == null) {
            throw new IllegalArgumentException("邮件正文不能为空");
        }
    }

    private MailAccount resolveMailAccount() {
        MailAccount account = mailAccount;
        if (account == null) {
            account = SpringUtils.getBean(MailAccount.class);
        }
        if (StringUtils.isAllBlank(from, user, pass)) {
            return account;
        }
        MailAccount copy = ObjectUtil.clone(account);
        copy.setFrom(StringUtils.blankToDefault(from, copy.getFrom()));
        copy.setUser(StringUtils.blankToDefault(user, copy.getUser()));
        copy.setPass(StringUtils.blankToDefault(pass, copy.getPass()));
        return copy;
    }

    private List<String> splitAddress(String addresses) {
        if (StrUtil.isBlank(addresses)) {
            return Collections.emptyList();
        }
        return normalizeAddresses(StrUtil.splitTrim(addresses.replace(';', ','), ','));
    }

    private List<String> normalizeAddresses(Collection<String> addresses) {
        if (CollUtil.isEmpty(addresses)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(addresses.size());
        for (String address : addresses) {
            if (StrUtil.isNotBlank(address)) {
                result.add(address.trim());
            }
        }
        return result;
    }

}
