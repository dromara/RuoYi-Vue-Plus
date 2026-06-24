import type { <#if !table.tree>PageResult, </#if> R } from '@/api/types';
import request from '@/api/request';
import type { ${BusinessName}Form, ${BusinessName}Query, ${BusinessName}VO } from './types';

/**
 * 查询${functionName}列表
 */
export function list${BusinessName}(query?: ${BusinessName}Query) {
  return request<R<<#if table.tree>${BusinessName}VO[]<#else> PageResult<${BusinessName}VO></#if>>>({
    url: '/${moduleName}/${businessName}/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询${functionName}详细
 */
export function get${BusinessName}(${pkColumn.javaField}: string | number) {
  return request<R<${BusinessName}VO>>({
    url: '/${moduleName}/${businessName}/' + ${pkColumn.javaField},
    method: 'get'
  });
}

/**
 * 新增${functionName}
 */
export function add${BusinessName}(data: ${BusinessName}Form) {
  return request<R>({
    url: '/${moduleName}/${businessName}',
    method: 'post',
    data
  });
}

/**
 * 修改${functionName}
 */
export function update${BusinessName}(data: ${BusinessName}Form) {
  return request<R>({
    url: '/${moduleName}/${businessName}',
    method: 'put',
    data
  });
}

<#if enableStatus>
/**
 * 修改${functionName}状态
 */
export function change${BusinessName}Status(
  ${pkColumn.javaField}: string | number,
  ${statusField}: <#if statusColumn.javaType == 'Boolean'>boolean<#elseif statusColumn.javaType == 'Integer' || statusColumn.javaType == 'Long'>number<#else> string</#if>
) {
  return request<R>({
    url: '/${moduleName}/${businessName}/changeStatus',
    method: 'put',
    data: {
      ${pkColumn.javaField},
      ${statusField}
    }
  });
}

</#if>
<#if enableSort>
/**
 * 调整${functionName}排序
 */
export function update${BusinessName}Sort(
  ${pkColumn.javaField}: string | number,
  ${sortField}: <#if sortColumn.javaType == 'LocalDateTime' || sortColumn.javaType == 'String'>string<#else> number</#if>
) {
  return request<R>({
    url: '/${moduleName}/${businessName}/updateSort',
    method: 'put',
    data: {
      ${pkColumn.javaField},
      ${sortField}
    }
  });
}

</#if>
/**
 * 删除${functionName}
 */
export function del${BusinessName}(${pkColumn.javaField}: string | number | Array<string | number>) {
  return request<R>({
    url: '/${moduleName}/${businessName}/' + ${pkColumn.javaField},
    method: 'delete'
  });
}
