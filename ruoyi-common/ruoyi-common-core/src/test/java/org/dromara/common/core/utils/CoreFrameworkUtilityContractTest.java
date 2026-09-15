package org.dromara.common.core.utils;

import cn.hutool.core.lang.Dict;
import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.dromara.common.core.utils.reflect.AnnotationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("common-core 框架工具契约单元测试")
class CoreFrameworkUtilityContractTest {

    private static Converter converter;
    private static Validator validator;
    private static StaticMessageSource messageSource;

    /**
     * 初始化框架工具静态依赖的最小 Spring 容器，确保测试不需要启动完整应用。
     */
    @BeforeAll
    static void initializeFrameworkUtilities() {
        converter = mock(Converter.class);
        validator = mock(Validator.class);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("converter", converter);
        context.getBeanFactory().registerSingleton("validator", validator);
        messageSource = context.getStaticMessageSource();
        messageSource.addMessage("welcome", Locale.SIMPLIFIED_CHINESE, "欢迎 {0}");
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 清理线程语言环境和 mock 调用记录，避免测试之间共享状态。
     */
    @AfterEach
    void resetFrameworkState() {
        LocaleContextHolder.resetLocaleContext();
        reset(converter, validator);
    }

    /**
     * 验证 Mapstruct Plus 的对象转换和目标对象填充均委托给框架 Converter。
     */
    @Test
    @DisplayName("委托对象转换和目标填充")
    void shouldDelegateObjectConversions() {
        Source source = new Source("alice");
        Destination converted = new Destination("converted");
        Destination target = new Destination("existing");
        when(converter.convert(source, Destination.class)).thenReturn(converted);
        when(converter.convert(source, target)).thenReturn(target);

        assertSame(converted, MapstructUtils.convert(source, Destination.class));
        assertSame(target, MapstructUtils.convert(source, target));
        verify(converter).convert(source, Destination.class);
        verify(converter).convert(source, target);
    }

    /**
     * 验证对象转换在来源或目标为空时直接返回空值，不误调用底层 Converter。
     */
    @Test
    @DisplayName("短路空对象转换")
    void shouldShortCircuitNullObjectConversions() {
        Source source = new Source("alice");

        assertNull(MapstructUtils.convert((Source) null, Destination.class));
        assertNull(MapstructUtils.convert(source, (Class<Destination>) null));
        assertNull(MapstructUtils.convert(source, (Destination) null));
        assertNull(MapstructUtils.convert((Source) null, new Destination("existing")));
        verifyNoInteractions(converter);
    }

    /**
     * 验证列表转换保留 null、空列表和普通列表各自约定的返回语义。
     */
    @Test
    @DisplayName("转换列表并处理空输入")
    void shouldConvertListsAndHandleEmptyInputs() {
        List<Source> sources = List.of(new Source("alice"));
        List<Destination> targets = List.of(new Destination("converted"));
        when(converter.convert(sources, Destination.class)).thenReturn(targets);

        assertNull(MapstructUtils.convert((List<Source>) null, Destination.class));
        assertEquals(List.of(), MapstructUtils.convert(List.<Source>of(), Destination.class));
        assertSame(targets, MapstructUtils.convert(sources, Destination.class));
        verify(converter).convert(sources, Destination.class);
    }

    /**
     * 验证 Map 转 Bean 仅在数据和目标类型有效时调用 Mapstruct Plus。
     */
    @Test
    @DisplayName("转换 Map 并处理无效输入")
    void shouldConvertMapsAndHandleInvalidInputs() {
        Map<String, Object> source = Map.of("name", "alice");
        Destination target = new Destination("converted");
        when(converter.convert(source, Destination.class)).thenReturn(target);

        assertNull(MapstructUtils.convert((Map<String, Object>) null, Destination.class));
        assertNull(MapstructUtils.convert(Map.of(), Destination.class));
        assertNull(MapstructUtils.convert(source, null));
        assertSame(target, MapstructUtils.convert(source, Destination.class));
        verify(converter).convert(source, Destination.class);
    }

    /**
     * 验证安全 Getter 只在对象和函数均有效时求值，并在无法求值时返回约定默认值。
     */
    @Test
    @DisplayName("安全读取对象属性")
    void shouldReadObjectPropertiesSafely() {
        Source source = new Source("alice");

        assertEquals("alice", ObjectUtils.notNullGetter(source, Source::name));
        assertNull(ObjectUtils.notNullGetter(null, Source::name));
        assertNull(ObjectUtils.notNullGetter(source, null));
        assertEquals("fallback", ObjectUtils.notNullGetter(null, Source::name, "fallback"));
        assertEquals("fallback", ObjectUtils.notNullGetter(source, null, "fallback"));
        assertEquals("alice", ObjectUtils.notNullGetter(source, Source::name, "fallback"));
        assertEquals("alice", ObjectUtils.notNull("alice", "fallback"));
        assertEquals("fallback", ObjectUtils.notNull(null, "fallback"));
    }

    /**
     * 验证 Bean Validation 在无约束违规时正常返回并传递指定校验组。
     */
    @Test
    @DisplayName("通过 Bean Validation 校验有效对象")
    void shouldValidateObjectWithRequestedGroups() {
        Source source = new Source("alice");
        when(validator.validate(source, ValidationGroup.class)).thenReturn(Set.of());

        assertDoesNotThrow(() -> ValidatorUtils.validate(source, ValidationGroup.class));
        verify(validator).validate(source, ValidationGroup.class);
    }

    /**
     * 验证 Bean Validation 对空对象和约束违规分别抛出框架约定的异常。
     */
    @Test
    @DisplayName("报告空对象和约束违规")
    void shouldReportNullObjectAndConstraintViolations() {
        RuntimeException nullException = assertThrows(RuntimeException.class,
            () -> ValidatorUtils.validate(null));
        @SuppressWarnings("unchecked")
        ConstraintViolation<Source> violation = mock(ConstraintViolation.class);
        Source source = new Source("");
        when(validator.validate(source)).thenReturn(Set.of(violation));

        ConstraintViolationException validationException = assertThrows(ConstraintViolationException.class,
            () -> ValidatorUtils.validate(source));

        assertEquals("请求参数不能为空", nullException.getMessage());
        assertEquals("参数校验异常", validationException.getMessage());
        assertEquals(Set.of(violation), validationException.getConstraintViolations());
    }

    /**
     * 验证国际化工具使用线程当前语言环境和消息参数查询 MessageSource。
     */
    @Test
    @DisplayName("按当前语言环境解析国际化消息")
    void shouldResolveMessageUsingCurrentLocale() {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        LocaleContextHolder.setLocale(locale);
        assertEquals("欢迎 alice", MessageUtils.message("welcome", "alice"));
    }

    /**
     * 验证缺少国际化资源时返回消息键，保持调用方可读的降级结果。
     */
    @Test
    @DisplayName("缺少国际化资源时返回消息键")
    void shouldFallbackToCodeWhenMessageIsMissing() {
        assertEquals("missing.code", MessageUtils.message("missing.code"));
    }

    /**
     * 验证按注解全类名读取注解实例及属性字典，防止 Hutool 升级改变反射结果。
     */
    @Test
    @DisplayName("按全类名读取注解和属性")
    void shouldReadAnnotationAndValuesByClassName() {
        String annotationName = ContractMarker.class.getName();

        ContractMarker annotation = assertInstanceOf(ContractMarker.class,
            AnnotationUtils.getAnnotation(AnnotatedType.class, annotationName));
        Dict values = AnnotationUtils.getAnnotationValueMap(AnnotatedType.class, annotationName);

        assertEquals("core", annotation.value());
        assertEquals(3, annotation.level());
        assertNotNull(values);
        assertEquals("core", values.getStr("value"));
        assertEquals(3, values.getInt("level"));
    }

    /**
     * 验证注解类不存在或目标元素未标注时返回空值，不把反射细节泄漏给调用方。
     */
    @Test
    @DisplayName("处理缺失注解类型和未标注元素")
    void shouldHandleMissingAnnotationTypesAndValues() {
        assertNull(AnnotationUtils.getAnnotation(String.class, ContractMarker.class.getName()));
        assertNull(AnnotationUtils.getAnnotation(AnnotatedType.class, "missing.Annotation"));
        assertNull(AnnotationUtils.getAnnotationValueMap(AnnotatedType.class, "missing.Annotation"));
    }

    private interface ValidationGroup {
    }

    private record Source(String name) {
    }

    private record Destination(String name) {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface ContractMarker {

        String value();

        int level();
    }

    @ContractMarker(value = "core", level = 3)
    private static final class AnnotatedType {
    }
}
