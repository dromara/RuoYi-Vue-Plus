package org.dromara.common.mail;

import org.dromara.common.mail.core.MailBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailBuilderTest {

    @Test
    void sendsHtmlWithSelectedAccountAndRecipients() {
        var account = new cn.hutool.extra.mail.MailAccount();
        var transport = mock(cn.hutool.extra.mail.JakartaMail.class, RETURNS_SELF);
        when(transport.send()).thenReturn("message-id");
        try (var factory = mockStatic(cn.hutool.extra.mail.JakartaMail.class)) {
            factory.when(() -> cn.hutool.extra.mail.JakartaMail.create(account)).thenReturn(transport);
            String id = MailBuilder.of(account).to(List.of(" a@example.org ")).cc(List.of("cc@example.org"))
                .bcc(List.of("bcc@example.org")).subject("审批结果").html("<b>通过</b>").send();
            assertEquals("message-id", id);
            verify(transport).setUseGlobalSession(false);
            verify(transport).setTos("a@example.org");
            verify(transport).setCcs("cc@example.org");
            verify(transport).setBccs("bcc@example.org");
            verify(transport).setTitle("审批结果");
            verify(transport).setContent("<b>通过</b>");
            verify(transport).setHtml(true);
        }
    }

    @Test
    void shouldKeepQuotedRecipientNamesIntact() {
        String named = "\"Li, Ming; Sales\" <ming@example.com>";
        String recipients = named + "; ops@example.com, info@example.com";
        var mail = MailBuilder.of().to(recipients).cc(recipients).bcc(recipients);
        for (String field : List.of("tos", "ccs", "bccs")) {
            assertEquals(List.of(named, "ops@example.com", "info@example.com"), ReflectionTestUtils.getField(mail, field));
        }
    }

    @Test
    void shouldReleaseImageWhenSendValidationFails() throws Exception {
        InputStream image = mock();
        var mail = MailBuilder.of().image("logo", image);
        assertThrows(IllegalArgumentException.class, mail::send);
        verify(image).close();
    }

    @Test
    void shouldReleaseImageWhenLastReferenceIsReplaced() throws Exception {
        InputStream shared = mock();
        InputStream replacement = mock();
        var mail = MailBuilder.of().image("logo", shared).image("banner", shared);
        mail.image("logo", replacement);
        verify(shared, never()).close();
        mail.image("banner", replacement);
        verify(shared).close();
        verify(replacement, never()).close();
    }
}
