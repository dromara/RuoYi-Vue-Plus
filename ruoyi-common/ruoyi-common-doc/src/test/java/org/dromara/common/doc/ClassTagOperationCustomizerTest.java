package org.dromara.common.doc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import org.dromara.common.doc.core.customizer.ClassTagOperationCustomizer;
import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.JavadocProvider;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassTagOperationCustomizerTest {

    private final JavadocProvider provider = mock(JavadocProvider.class);
    private final ClassTagOperationCustomizer customizer = new ClassTagOperationCustomizer(Optional.of(provider), null);

    @Test
    void shouldLimitGeneratedTagsToCurrentGroup() throws Exception {
        documentOrders();
        OpenAPI billing = new OpenAPI().path("/billing", new PathItem().get(new Operation().addTagsItem("账单")));
        customizer.customise(billing);
        assertTrue(billing.getTags() == null || billing.getTags().stream().noneMatch(tag -> "订单".equals(tag.getName())));
    }

    @Test
    void shouldKeepAutoTagStillUsedByAnotherOperation() throws Exception {
        OpenAPI api = documentOrders();
        String original = OpenAPIService.splitCamelCase(OrderController.class.getSimpleName());
        api.path("/history", new PathItem().get(new Operation().addTagsItem(original)));
        Tag history = new Tag().name(original).description("历史查询");
        api.addTagsItem(history);
        customizer.customise(api);
        assertTrue(api.getTags().contains(history));
    }

    @Test
    void shouldCompleteExistingTagDescription() throws Exception {
        OpenAPI api = documentOrders();
        api.addTagsItem(new Tag().name("订单"));
        customizer.customise(api);
        assertEquals(List.of("订单\n订单查询"), api.getTags().stream().map(Tag::getDescription).toList());
    }

    private OpenAPI documentOrders() throws Exception {
        when(provider.getClassJavadoc(OrderController.class)).thenReturn("订单\n订单查询");
        var handler = new HandlerMethod(new OrderController(), OrderController.class.getDeclaredMethod("list"));
        var operation = customizer.customize(new Operation(), handler);
        return new OpenAPI().paths(new Paths().addPathItem("/orders", new PathItem().get(operation)));
    }

    private static class OrderController {
        public void list() {
        }
    }
}
