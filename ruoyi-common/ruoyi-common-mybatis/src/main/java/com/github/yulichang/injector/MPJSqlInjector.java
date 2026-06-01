package com.github.yulichang.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.SelectList;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.github.yulichang.adapter.base.tookit.VersionUtils;
import com.github.yulichang.base.JoinMapper;
import com.github.yulichang.method.*;
import com.github.yulichang.toolkit.MPJTableMapperHelper;
import com.github.yulichang.toolkit.ReflectionKit;
import com.github.yulichang.toolkit.TableHelper;
import lombok.Getter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * SQL 注入器
 *
 * @author yulichang
 * @see DefaultSqlInjector
 */
@Getter
public class MPJSqlInjector extends DefaultSqlInjector {

    /**
     * 原始 SQL 注入器，用于兼容项目自定义注入逻辑。
     */
    private AbstractSqlInjector sqlInjector;

    /**
     * 构造 MPJ SQL 注入器。
     */
    public MPJSqlInjector() {
    }

    /**
     * 构造带原始注入器的 MPJ SQL 注入器。
     *
     * @param sqlInjector 原始 SQL 注入器
     */
    public MPJSqlInjector(ISqlInjector sqlInjector) {
        if (Objects.nonNull(sqlInjector) && sqlInjector instanceof AbstractSqlInjector) {
            this.sqlInjector = (AbstractSqlInjector) sqlInjector;
        }
    }

    /**
     * 获取 Mapper 可用的注入方法列表。
     *
     * @param configuration MyBatis 配置
     * @param mapperClass   Mapper 类型
     * @param tableInfo     表信息
     * @return 注入方法列表
     */
    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        if (!isJoinMapper(mapperClass)) {
            if (Objects.nonNull(sqlInjector)) {
                return sqlInjector.getMethodList(configuration, mapperClass, tableInfo);
            }
            return super.getMethodList(configuration, mapperClass, tableInfo);
        }
        if (Objects.nonNull(sqlInjector)) {
            return methodFilter(sqlInjector.getMethodList(configuration, mapperClass, tableInfo));
        }
        return methodFilter(super.getMethodList(configuration, mapperClass, tableInfo));
    }

    /**
     * 过滤并追加 MPJ 需要的 SQL 注入方法。
     *
     * @param list 原始注入方法列表
     * @return 过滤后的注入方法列表
     */
    private List<AbstractMethod> methodFilter(List<AbstractMethod> list) {
        String packageStr = SelectList.class.getPackage().getName();
        List<String> methodList = Arrays.asList(
            "Update",
            "Delete",
            "SelectOne",
            "SelectCount",
            "SelectMaps",
            "SelectMapsPage",
            "SelectObjs",
            "SelectList",
            "SelectPage");
        list.removeIf(i -> methodList.contains(i.getClass().getSimpleName()) &&
            Objects.equals(packageStr, i.getClass().getPackage().getName()));
        addAll(list, getWrapperMethod());
        addAll(list, getJoinMethod());
        return list;
    }

    /**
     * 获取 MPJ 联表操作注入方法。
     *
     * @return 联表操作注入方法列表
     */
    private List<AbstractMethod> getJoinMethod() {
        List<AbstractMethod> list = new ArrayList<>();
        if (VersionUtils.compare(VersionUtils.getVersion(), "3.5.0") >= 0) {
            list.add(new DeleteJoin(SqlMethod.DELETE_JOIN.getMethod()));
            list.add(new UpdateJoin(SqlMethod.UPDATE_JOIN.getMethod()));
            list.add(new UpdateJoinAndNull(SqlMethod.UPDATE_JOIN_AND_NULL.getMethod()));
            list.add(new SelectJoinCount(SqlMethod.SELECT_JOIN_COUNT.getMethod()));
            list.add(new SelectJoinOne(SqlMethod.SELECT_JOIN_ONE.getMethod()));
            list.add(new SelectJoinList(SqlMethod.SELECT_JOIN_LIST.getMethod()));
            list.add(new SelectJoinPage(SqlMethod.SELECT_JOIN_PAGE.getMethod()));
        } else {
            list.add(new DeleteJoin());
            list.add(new UpdateJoin());
            list.add(new UpdateJoinAndNull());
            list.add(new SelectJoinCount());
            list.add(new SelectJoinOne());
            list.add(new SelectJoinList());
            list.add(new SelectJoinPage());
        }
        return list;
    }

    /**
     * 获取 MPJ 覆盖 MyBatis-Plus 默认 Wrapper 的注入方法。
     *
     * @return Wrapper 注入方法列表
     */
    private List<AbstractMethod> getWrapperMethod() {
        List<AbstractMethod> list = new ArrayList<>();
        list.add(new com.github.yulichang.method.mp.Delete());
        list.add(new com.github.yulichang.method.mp.SelectOne());
        list.add(new com.github.yulichang.method.mp.SelectCount());
        list.add(new com.github.yulichang.method.mp.SelectMaps());
        list.add(new com.github.yulichang.method.mp.SelectMapsPage());
        list.add(new com.github.yulichang.method.mp.SelectObjs());
        list.add(new com.github.yulichang.method.mp.SelectList());
        list.add(new com.github.yulichang.method.mp.SelectPage());
        list.add(new com.github.yulichang.method.mp.Update());
        return list;
    }

    /**
     * 将新增注入方法追加到原始列表中，已存在同名方法时不重复追加。
     *
     * @param source  原始方法列表
     * @param addList 待追加方法列表
     */
    private void addAll(List<AbstractMethod> source, List<AbstractMethod> addList) {
        for (AbstractMethod method : addList) {
            if (source.stream().noneMatch(m -> m.getClass().getSimpleName().equals(method.getClass().getSimpleName()))) {
                source.add(method);
            }
        }
    }

    /**
     * 初始化 Mapper 注入信息，并为 JoinMapper 注册 MPJ 表映射缓存。
     *
     * @param builderAssistant Mapper 构建助手
     * @param mapperClass      Mapper 类型
     */
    @Override
    public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        super.inspectInject(builderAssistant, mapperClass);
        if (!isJoinMapper(mapperClass)) {
            return;
        }
        Class<?> modelClass = ReflectionKit.getSuperClassGenericType(mapperClass, Mapper.class, 0);
        MPJTableMapperHelper.init(modelClass, mapperClass);
        Supplier<Class<?>> supplier = () -> {
            try {
                return extractModelClassOld(mapperClass);
            } catch (Throwable throwable) {
                return null;
            }
        };
        TableHelper.init(modelClass, supplier.get());
    }

    /**
     * 兼容旧版泛型解析逻辑，提取 Mapper 绑定的实体类型。
     *
     * @param mapperClass Mapper 类型
     * @return Mapper 泛型中的实体类型，无法解析时返回 null
     */
    @SuppressWarnings("IfStatementWithIdenticalBranches")
    protected Class<?> extractModelClassOld(Class<?> mapperClass) {
        Type[] types = mapperClass.getGenericInterfaces();
        ParameterizedType target = null;
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type[] typeArray = ((ParameterizedType) type).getActualTypeArguments();
                if (ArrayUtils.isNotEmpty(typeArray)) {
                    for (Type t : typeArray) {
                        if (t instanceof TypeVariable || t instanceof WildcardType) {
                            break;
                        } else {
                            target = (ParameterizedType) type;
                            break;
                        }
                    }
                }
                break;
            }
        }
        return target == null ? null : (Class<?>) target.getActualTypeArguments()[0];
    }

    /**
     * 判断 Mapper 是否继承 MPJ JoinMapper。
     *
     * @param mapperClass Mapper 类型
     * @return true 是 JoinMapper false 不是 JoinMapper
     */
    private boolean isJoinMapper(Class<?> mapperClass) {
        return JoinMapper.class.isAssignableFrom(mapperClass);
    }
}
