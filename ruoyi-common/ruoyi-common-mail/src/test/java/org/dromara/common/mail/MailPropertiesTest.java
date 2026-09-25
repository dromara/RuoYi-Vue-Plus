package org.dromara.common.mail;

import cn.hutool.extra.mail.MailAccount;
import org.dromara.common.mail.config.properties.MailProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("common-mail 功能单元测试")
class MailPropertiesTest {

    /**
     * 验证业务邮件属性能够完整转换为 Hutool 邮件账户，避免发送时遗漏安全和超时配置。
     */
    @Test
    @DisplayName("转换邮件账户配置")
    void shouldConvertPropertiesToMailAccount() {
        MailProperties properties = new MailProperties();
        properties.setHost("smtp.example.com");
        properties.setPort(465);
        properties.setAuth(true);
        properties.setUser("sender@example.com");
        properties.setPass("secret");
        properties.setFrom("Sender <sender@example.com>");
        properties.setStarttlsEnable(true);
        properties.setSslEnable(true);
        properties.setTimeout(5000L);
        properties.setConnectionTimeout(3000L);

        MailAccount account = properties.toMailAccount();

        assertEquals("smtp.example.com", account.getHost());
        assertEquals(465, account.getPort());
        assertTrue(account.isAuth());
        assertEquals("sender@example.com", account.getUser());
        assertEquals("secret", account.getPass());
        assertEquals("Sender <sender@example.com>", account.getFrom());
        assertTrue(account.isStarttlsEnable());
        assertTrue(account.isSslEnable());
        assertEquals(5000L, ReflectionTestUtils.getField(account, "timeout"));
        assertEquals(3000L, ReflectionTestUtils.getField(account, "connectionTimeout"));
    }
}
