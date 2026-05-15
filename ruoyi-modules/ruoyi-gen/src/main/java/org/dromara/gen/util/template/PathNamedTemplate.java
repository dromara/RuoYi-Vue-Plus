package org.dromara.gen.util.template;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateEngine;
import lombok.Getter;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 基于路径命名的模板委托实现
 *
 * @author 秋辞未寒
 */
public class PathNamedTemplate implements Template {

    @Getter
    private final String pathName;

    private final Template delegate;

    private PathNamedTemplate(String pathName, Template delegate) {
        this.pathName = pathName;
        this.delegate = delegate;
    }

    @Override
    public void render(Map<?, ?> bindingMap, Writer writer) {
        delegate.render(bindingMap, writer);
    }

    @Override
    public void render(Map<?, ?> bindingMap, OutputStream out) {
        delegate.render(bindingMap, out);
    }

    @Override
    public void render(Map<?, ?> bindingMap, File file) {
        delegate.render(bindingMap, file);
    }

    @Override
    public String render(Map<?, ?> bindingMap) {
        return delegate.render(bindingMap);
    }

    /**
     * 构建带路径名称的模板。
     *
     * @param pathName 路径名称
     * @param delegate 模板对象
     * @return 带路径名称的模板
     */
    public static PathNamedTemplate form(String pathName, Template delegate) {
        return new PathNamedTemplate(pathName, delegate);
    }

    /**
     * 根据模板引擎和路径名称构建模板。
     *
     * @param templateEngine 模板引擎
     * @param pathName       路径名称
     * @return 带路径名称的模板
     */
    public static PathNamedTemplate form(TemplateEngine templateEngine,String pathName) {
        return new PathNamedTemplate(pathName,templateEngine.getTemplate(pathName));
    }

    /**
     * 根据模板引擎和路径名称集合构建模板映射。
     *
     * @param templateEngine 模板引擎
     * @param pathNames      路径名称集合
     * @return 模板映射
     */
    public static Map<String, PathNamedTemplate> form(TemplateEngine templateEngine, Set<String> pathNames) {
        Map<String, PathNamedTemplate> result = new HashMap<>();
        for (String pathName : pathNames) {
            PathNamedTemplate pathNamedTemplate = form(templateEngine, pathName);
            result.put(pathName, pathNamedTemplate);
        }
        return result;
    }

}
