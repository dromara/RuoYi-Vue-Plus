package org.dromara.common.doc.core.resolver;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ClassLoaderUtil;
import io.swagger.v3.oas.models.Operation;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.doc.core.model.SaTokenSecurityMetadata;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 基于JavaDoc的SaToken权限解析器
 *
 * @author echo
 * @author 秋辞未寒
 */
@SuppressWarnings("unchecked")
@Slf4j
public class SaTokenAnnotationMetadataJavadocResolver extends AbstractMetadataJavadocResolver<SaTokenSecurityMetadata> {

    /**
     * 默认元数据提供者，每次解析都会创建一个新的元数据对象
     */
    public static final Supplier<SaTokenSecurityMetadata> DEFAULT_METADATA_PROVIDER = SaTokenSecurityMetadata::new;

    /**
     * Sa-Token 注解包名。
     */
    private static final String BASE_CLASS_NAME = "cn.dev33.satoken.annotation";

    /**
     * Sa-Token 角色校验注解类名。
     */
    private static final String SA_CHECK_ROLE_CLASS_NAME = BASE_CLASS_NAME + ".SaCheckRole";

    /**
     * Sa-Token 权限校验注解类名。
     */
    private static final String SA_CHECK_PERMISSION_CLASS_NAME = BASE_CLASS_NAME + ".SaCheckPermission";

    /**
     * Sa-Token 忽略校验注解类名。
     */
    private static final String SA_IGNORE_CLASS_NAME = BASE_CLASS_NAME + ".SaIgnore";

    /**
     * Sa-Token 登录校验注解类名。
     */
    private static final String SA_CHECK_LOGIN_NAME = BASE_CLASS_NAME + ".SaCheckLogin";

    /**
     * Sa-Token 角色校验注解类型。
     */
    private static final Class<? extends Annotation> SA_CHECK_ROLE_CLASS;

    /**
     * Sa-Token 权限校验注解类型。
     */
    private static final Class<? extends Annotation> SA_CHECK_PERMISSION_CLASS;

    /**
     * Sa-Token 忽略校验注解类型。
     */
    private static final Class<? extends Annotation> SA_IGNORE_CLASS;

    /**
     * Sa-Token 登录校验注解类型。
     */
    private static final Class<? extends Annotation> SA_CHECK_LOGIN_CLASS;


    static {
        // 通过类加载器去加载注解类Class实例
        SA_CHECK_ROLE_CLASS = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(SA_CHECK_ROLE_CLASS_NAME, false);
        SA_CHECK_PERMISSION_CLASS = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(SA_CHECK_PERMISSION_CLASS_NAME, false);
        SA_IGNORE_CLASS = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(SA_IGNORE_CLASS_NAME, false);
        SA_CHECK_LOGIN_CLASS = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(SA_CHECK_LOGIN_NAME, false);
        if (log.isDebugEnabled()) {
            log.debug("SaTokenAnnotationJavadocResolver init success, load annotation class: {}", List.of(SA_CHECK_ROLE_CLASS, SA_CHECK_PERMISSION_CLASS, SA_IGNORE_CLASS, SA_CHECK_LOGIN_CLASS));
        }
    }

    /**
     * 构造 Sa-Token 权限解析器。
     */
    public SaTokenAnnotationMetadataJavadocResolver() {
        this(DEFAULT_METADATA_PROVIDER);
    }

    /**
     * 使用自定义元数据提供者创建解析器。
     *
     * @param metadataProvider 元数据提供者
     */
    public SaTokenAnnotationMetadataJavadocResolver(Supplier<SaTokenSecurityMetadata> metadataProvider) {
        super(metadataProvider);
    }

    /**
     * 使用指定顺序创建解析器。
     *
     * @param order 顺序值
     */
    public SaTokenAnnotationMetadataJavadocResolver(int order) {
        this(DEFAULT_METADATA_PROVIDER,order);
    }

    /**
     * 使用自定义元数据提供者和顺序创建解析器。
     *
     * @param metadataProvider 元数据提供者
     * @param order 顺序值
     */
    public SaTokenAnnotationMetadataJavadocResolver(Supplier<SaTokenSecurityMetadata> metadataProvider, int order) {
        super(metadataProvider,order);
    }

    /**
     * 判断当前方法是否需要由该解析器处理。
     *
     * @param handlerMethod Handler 方法
     * @return 是否支持
     */
    @Override
    public boolean supports(HandlerMethod handlerMethod) {
        return hasAnnotation(handlerMethod, SA_CHECK_ROLE_CLASS) || hasAnnotation(handlerMethod, SA_CHECK_PERMISSION_CLASS) || hasAnnotation(handlerMethod, SA_IGNORE_CLASS);
    }

    /**
     * 解析 Sa-Token 注解并转换为文档说明。
     *
     * @param handlerMethod Handler 方法
     * @param operation OpenAPI 操作对象
     * @param metadata 权限元数据
     * @return Markdown 描述
     */
    @Override
    public String resolve(HandlerMethod handlerMethod, Operation operation, SaTokenSecurityMetadata metadata) {
        // 检查是否忽略校验
        if(hasAnnotation(handlerMethod, SA_IGNORE_CLASS_NAME)){
            metadata.setIgnore(true);
            return metadata.toMarkdownString();
        }

        // 解析权限校验
        resolvePermissionCheck(handlerMethod, metadata);

        // 解析角色校验
        resolveRoleCheck(handlerMethod, metadata);
        return metadata.toMarkdownString();
    }

    /**
     * 解析权限校验
     */
    private void resolvePermissionCheck(HandlerMethod handlerMethod, SaTokenSecurityMetadata metadata) {
        // 解析获取方法上的注解角色信息
        if (hasMethodAnnotation(handlerMethod, SA_CHECK_PERMISSION_CLASS_NAME)) {
            Map<String, Object> annotationValueMap = getMethodAnnotationValueMap(handlerMethod, SA_CHECK_PERMISSION_CLASS);
            resolvePermissionAnnotation(metadata, annotationValueMap);
        }
        // 解析获取类上的注解角色信息
        if (hasClassAnnotation(handlerMethod, SA_CHECK_PERMISSION_CLASS_NAME)) {
            Map<String, Object> annotationValueMap = getClassAnnotationValueMap(handlerMethod, SA_CHECK_PERMISSION_CLASS);
            resolvePermissionAnnotation(metadata, annotationValueMap);
        }
    }

    /**
     * 解析权限注解
     */
    private void resolvePermissionAnnotation(SaTokenSecurityMetadata metadata, Map<String, Object> annotationValueMap) {
        try {
            // 反射获取注解属性
            Object value = annotationValueMap.get( "value");
            Object mode = annotationValueMap.get( "mode");
            Object type = annotationValueMap.get( "type");
            Object orRole = annotationValueMap.get( "orRole");

            String[] values = Convert.toStrArray(value);
            String modeStr = mode != null ? mode.toString() : "AND";
            String typeStr = type != null ? type.toString() : "";
            String[] orRoles = Convert.toStrArray(orRole);

            metadata.addPermission(values, modeStr, typeStr, orRoles);
        } catch (Exception ignore) {
            // 忽略解析错误
        }
    }

    /**
     * 解析角色校验
     */
    private void resolveRoleCheck(HandlerMethod handlerMethod, SaTokenSecurityMetadata metadata) {
        // 解析获取方法上的注解角色信息
        if (hasMethodAnnotation(handlerMethod, SA_CHECK_ROLE_CLASS_NAME)) {
            Map<String, Object> annotationValueMap = getMethodAnnotationValueMap(handlerMethod, SA_CHECK_ROLE_CLASS);
            resolveRoleAnnotation(metadata, annotationValueMap);
        }
        // 解析获取类上的注解角色信息
        if (hasClassAnnotation(handlerMethod, SA_CHECK_ROLE_CLASS_NAME)) {
            Map<String, Object> annotationValueMap = getClassAnnotationValueMap(handlerMethod, SA_CHECK_ROLE_CLASS);
            resolveRoleAnnotation(metadata, annotationValueMap);
        }
    }

    /**
     * 解析角色注解
     */
    private void resolveRoleAnnotation(SaTokenSecurityMetadata metadata, Map<String, Object> annotationValueMap) {
        try {
            // 反射获取注解属性
            Object value = annotationValueMap.get("value");
            Object mode = annotationValueMap.get("mode");
            Object type = annotationValueMap.get("type");

            String[] values = Convert.toStrArray(value);
            String modeStr = mode != null ? mode.toString() : "AND";
            String typeStr = type != null ? type.toString() : "";

            metadata.addRole(values, modeStr, typeStr);
        } catch (Exception ignore) {
            // 忽略解析错误
        }
    }

}
