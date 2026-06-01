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

    private AbstractSqlInjector sqlInjector;

    public MPJSqlInjector() {
    }

    public MPJSqlInjector(ISqlInjector sqlInjector) {
        if (Objects.nonNull(sqlInjector) && sqlInjector instanceof AbstractSqlInjector) {
            this.sqlInjector = (AbstractSqlInjector) sqlInjector;
        }
    }

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

    private void addAll(List<AbstractMethod> source, List<AbstractMethod> addList) {
        for (AbstractMethod method : addList) {
            if (source.stream().noneMatch(m -> m.getClass().getSimpleName().equals(method.getClass().getSimpleName()))) {
                source.add(method);
            }
        }
    }

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

    private boolean isJoinMapper(Class<?> mapperClass) {
        return JoinMapper.class.isAssignableFrom(mapperClass);
    }
}
