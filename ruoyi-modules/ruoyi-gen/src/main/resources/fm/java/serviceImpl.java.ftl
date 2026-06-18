package ${packageName}.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
<#if table.crud>
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
</#if>
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
<#if enableUnique>
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
</#if>
import org.dromara.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ${packageName}.domain.bo.${ClassName}Bo;
import ${packageName}.domain.vo.${ClassName}Vo;
import ${packageName}.domain.${ClassName};
import ${packageName}.mapper.${ClassName}Mapper;
import ${packageName}.service.I${ClassName}Service;
<#if table.tree>
import org.dromara.common.core.exception.ServiceException;
</#if>

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * ${functionName}Service业务层处理
 *
 * @author ${author}
 * @date ${datetime}
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ${ClassName}ServiceImpl implements I${ClassName}Service {

    private final ${ClassName}Mapper ${className}Mapper;

/**
     * 查询${functionName}
     *
     * @param ${pkColumn.javaField} 主键
     * @return ${functionName}
     */
    @Override
    public ${ClassName}Vo queryById(${pkColumn.javaType} ${pkColumn.javaField}) {
        return ${className}Mapper.selectVoById(${pkColumn.javaField});
    }

<#if table.crud>
    /**
     * 分页查询${functionName}列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return ${functionName}分页列表
     */
    @Override
    public PageResult<${ClassName}Vo> queryPageList(${ClassName}Bo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<${ClassName}> lqw = buildQueryWrapper(bo);
        Page<${ClassName}Vo> result = ${className}Mapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }
</#if>

    /**
     * 查询符合条件的${functionName}列表
     *
     * @param bo 查询条件
     * @return ${functionName}列表
     */
    @Override
    public List<${ClassName}Vo> queryList(${ClassName}Bo bo) {
        LambdaQueryWrapper<${ClassName}> lqw = buildQueryWrapper(bo);
        return ${className}Mapper.selectVoList(lqw);
    }

<#if enableUnique>
    /**
     * 校验${functionName}是否满足组合唯一约束
     *
     * @param bo ${functionName}
     * @return 是否唯一
     */
    @Override
    public boolean checkUnique(${ClassName}Bo bo) {
        boolean hasUniqueValue = true;
<#list uniqueColumns as column>
<#if column.javaType == 'String'>
        hasUniqueValue = hasUniqueValue && StringUtils.isNotBlank(bo.get${column.capJavaField}());
<#else>
        hasUniqueValue = hasUniqueValue && bo.get${column.capJavaField}() != null;
</#if>
</#list>
        if (!hasUniqueValue) {
            return true;
        }
        LambdaQueryWrapper<${ClassName}> lqw = Wrappers.lambdaQuery();
<#list uniqueColumns as column>
        lqw.eq(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}());
</#list>
        lqw.ne(bo.get${pkColumn.capJavaField}() != null, ${ClassName}::get${pkColumn.capJavaField}, bo.get${pkColumn.capJavaField}());
        return !${className}Mapper.exists(lqw);
    }
</#if>

    private LambdaQueryWrapper<${ClassName}> buildQueryWrapper(${ClassName}Bo bo) {
<#if hasBetween>
        Map<String, Object> params = bo.getParams();
</#if>
        return QueryBuilder.lambda(${ClassName}.class)
<#list columns as column>
<#if column.query>
<#assign queryType = column.queryType>
<#assign javaType = column.javaType>
<#assign AttrName = column.capJavaField>
<#assign mpMethod = column.queryType?lower_case>
<#if queryType != 'BETWEEN'>
<#if javaType == 'String'>
<#assign condition = 'StringUtils.isNotBlank(bo.get'+AttrName+'())'>
<#if queryType == 'LIKE'>
            .likeIfText(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'EQ'>
            .eqIfText(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'NE'>
            .neIfText(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#else>
            .${mpMethod}(${condition}, ${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
</#if>
<#else>
<#assign condition = 'bo.get'+AttrName+'() != null'>
<#if queryType == 'EQ'>
            .eqIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'NE'>
            .neIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'GT'>
            .gtIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'LT'>
            .ltIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#else>
            .${mpMethod}(${condition}, ${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
</#if>
</#if>
<#else>
            .betweenParams(${ClassName}::get${column.capJavaField}, params, "begin${column.capJavaField}", "end${column.capJavaField}")
</#if>
</#if>
</#list>
<#if table.tree && "" != treeAncestorsField>
            .orderByAsc(${ClassName}::get${treeAncestorsCap})
</#if>
<#if table.tree && "" != treeParentCode>
            .orderByAsc(${ClassName}::get${treeParentCap})
</#if>
<#if table.tree && "" != treeOrderField>
            .orderByAsc(${ClassName}::get${treeOrderCap})
<#elseif enableSort>
            .orderByAsc(${ClassName}::get${sortColumn.capJavaField})
</#if>
            .orderByAsc(${ClassName}::get${pkColumn.capJavaField})
            .build();
    }

    /**
     * 新增${functionName}
     *
     * @param bo ${functionName}
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(${ClassName}Bo bo) {
        ${ClassName} add = MapstructUtils.convert(bo, ${ClassName}.class);
<#if table.tree>
        fillTreeMetaBeforeSave(add, false);
</#if>
        validEntityBeforeSave(add);
        boolean flag = ${className}Mapper.insert(add) > 0;
        if (flag) {
            bo.set${pkColumn.capJavaField}(add.get${pkColumn.capJavaField}());
        }
        return flag;
    }

    /**
     * 修改${functionName}
     *
     * @param bo ${functionName}
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(${ClassName}Bo bo) {
        ${ClassName} update = MapstructUtils.convert(bo, ${ClassName}.class);
<#if table.tree>
        fillTreeMetaBeforeSave(update, true);
</#if>
        validEntityBeforeSave(update);
        return ${className}Mapper.updateById(update) > 0;
    }

<#if enableStatus>
    /**
     * 修改${functionName}状态
     *
     * @param ${pkColumn.javaField} 主键
     * @param status 状态值
     * @return 是否修改成功
     */
    @Override
    public Boolean updateStatus(${pkColumn.javaType} ${pkColumn.javaField}, ${statusColumn.javaType} status) {
        return ${className}Mapper.lambda()
            .set(${ClassName}::get${statusColumn.capJavaField}, status)
            .eq(${ClassName}::get${pkColumn.capJavaField}, ${pkColumn.javaField})
            .update();
    }
</#if>

<#if enableSort>
    /**
     * 调整${functionName}排序
     *
     * @param ${pkColumn.javaField} 主键
     * @param sortValue 排序值
     * @return 是否修改成功
     */
    @Override
    public Boolean updateSort(${pkColumn.javaType} ${pkColumn.javaField}, ${sortColumn.javaType} sortValue) {
        return ${className}Mapper.lambda()
            .set(${ClassName}::get${sortColumn.capJavaField}, sortValue)
            .eq(${ClassName}::get${pkColumn.capJavaField}, ${pkColumn.javaField})
            .update();
    }
</#if>

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(${ClassName} entity) {
        // 可在此扩展通用业务校验
    }

<#if table.tree>
    private void fillTreeMetaBeforeSave(${ClassName} entity, boolean updateMode) {
<#if "" != treeParentCode>
        if (entity.get${treeParentCap}() == null) {
            entity.set${treeParentCap}(${treeRootValueJavaLiteral});
        }
        if (ObjectUtil.equal(entity.get${pkColumn.capJavaField}(), entity.get${treeParentCap}())) {
            throw new ServiceException("${functionName}父节点不能选择自身");
        }
<#if "" != treeAncestorsField>
        ${ClassName} parent = null;
        if (!ObjectUtil.equal(entity.get${treeParentCap}(), ${treeRootValueJavaLiteral})) {
            parent = ${className}Mapper.selectById(entity.get${treeParentCap}());
            if (ObjectUtil.isNull(parent)) {
                throw new ServiceException("${functionName}父节点不存在");
            }
        }
        if (updateMode && entity.get${pkColumn.capJavaField}() != null && ObjectUtil.isNotNull(parent)
            && containsAncestor(parent.get${treeAncestorsCap}(), entity.get${pkColumn.capJavaField}())) {
            throw new ServiceException("不能选择当前节点或其子节点作为父节点");
        }
        String newAncestors = resolveAncestors(entity.get${treeParentCap}(), parent);
        if (updateMode && entity.get${pkColumn.capJavaField}() != null) {
            ${ClassName} oldEntity = ${className}Mapper.selectById(entity.get${pkColumn.capJavaField}());
            if (ObjectUtil.isNull(oldEntity)) {
                throw new ServiceException("${functionName}不存在，无法修改");
            }
            String oldAncestors = oldEntity.get${treeAncestorsCap}();
            entity.set${treeAncestorsCap}(newAncestors);
            if (!StringUtils.equals(oldAncestors, newAncestors)) {
                updateChildrenAncestors(entity.get${pkColumn.capJavaField}(), newAncestors, oldAncestors);
            }
        } else {
            entity.set${treeAncestorsCap}(newAncestors);
        }
</#if>
</#if>
    }
<#if "" != treeAncestorsField>

    private String resolveAncestors(${treeParentColumn.javaType} parentId, ${ClassName} parent) {
        if (ObjectUtil.equal(parentId, ${treeRootValueJavaLiteral})) {
            return "${treeRootValue}";
        }
        String parentAncestors = parent.get${treeAncestorsCap}();
        if (StringUtils.isBlank(parentAncestors)) {
            return String.valueOf(parentId);
        }
        return parentAncestors + StringUtils.SEPARATOR + parentId;
    }

    private void updateChildrenAncestors(${pkColumn.javaType} currentId, String newAncestors, String oldAncestors) {
        List<${ClassName}> children = ${className}Mapper.lambda()
            .select(${ClassName}::get${pkColumn.capJavaField}, ${ClassName}::get${treeAncestorsCap})
            .findInSet(currentId, ${ClassName}::get${treeAncestorsCap})
            .list();
        List<${ClassName}> updateList = new ArrayList<>();
        for (${ClassName} child : children) {
            String ancestors = child.get${treeAncestorsCap}();
            if (StringUtils.isBlank(ancestors)) {
                continue;
            }
            ${ClassName} update = new ${ClassName}();
            update.set${pkColumn.capJavaField}(child.get${pkColumn.capJavaField}());
            update.set${treeAncestorsCap}(StringUtils.replaceOnce(ancestors, oldAncestors, newAncestors));
            updateList.add(update);
        }
        if (!updateList.isEmpty()) {
            ${className}Mapper.updateBatchById(updateList);
        }
    }

    private boolean containsAncestor(String ancestors, ${pkColumn.javaType} nodeId) {
        for (String item : StringUtils.splitList(ancestors)) {
            if (StringUtils.equals(item, String.valueOf(nodeId))) {
                return true;
            }
        }
        return false;
    }
</#if>
</#if>

    /**
     * 校验并批量删除${functionName}信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<${pkColumn.javaType}> ids, Boolean isValid) {
        if (isValid) {
            // 可在此扩展删除前业务校验
        }
        return ${className}Mapper.deleteByIds(ids) > 0;
    }
}


