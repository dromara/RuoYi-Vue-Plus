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

    /**
     * 创建基于路径命名的模板。
     *
     * @param pathName 路径名称
     * @param delegate 委托模板
     */
    private PathNamedTemplate(String pathName, Template delegate) {
        this.pathName = pathName;
        this.delegate = delegate;
    }

    /**
     * 渲染模板到字符输出器。
     *
     * @param bindingMap 模板绑定数据
     * @param writer     字符输出器
     */
    @Override
    public void render(Map<?, ?> bindingMap, Writer writer) {
        delegate.render(bindingMap, writer);
    }

    /**
     * 渲染模板到输出流。
     *
     * @param bindingMap 模板绑定数据
     * @param out        输出流
     */
    @Override
    public void render(Map<?, ?> bindingMap, OutputStream out) {
        delegate.render(bindingMap, out);
    }

    /**
     * 渲染模板到文件。
     *
     * @param bindingMap 模板绑定数据
     * @param file       输出文件
     */
    @Override
    public void render(Map<?, ?> bindingMap, File file) {
        delegate.render(bindingMap, file);
    }

    /**
     * 渲染模板为字符串。
     *
     * @param bindingMap 模板绑定数据
     * @return 渲染结果
     */
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
    public static PathNamedTemplate form(TemplateEngine templateEngine, String pathName) {
        return new PathNamedTemplate(pathName, templateEngine.getTemplate(pathName));
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
