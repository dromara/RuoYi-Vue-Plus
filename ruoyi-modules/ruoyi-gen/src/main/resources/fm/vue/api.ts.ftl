import type { ${BusinessName}Form, ${BusinessName}Query, ${BusinessName}VO } from '@/api/${moduleName}/${businessName}/types';
<#if !table.tree>
import type { PageResult } from '@/api/types';
</#if>
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

/**
 * 查询${functionName}列表
 * @param query
 * @returns {*}
 */
export const list${BusinessName} = (query?: ${BusinessName}Query): AxiosPromise<<#if table.tree>${BusinessName}VO[]<#else>PageResult<${BusinessName}VO></#if>> => {
  return request({
    url: '/${moduleName}/${businessName}/list',
    method: 'get',
    params: query
  });
};

/**
 * 查询${functionName}详细
 * @param ${pkColumn.javaField}
 */
export const get${BusinessName} = (${pkColumn.javaField}: string | number): AxiosPromise<${BusinessName}VO> => {
  return request({
    url: '/${moduleName}/${businessName}/' + ${pkColumn.javaField},
    method: 'get'
  });
};

/**
 * 新增${functionName}
 * @param data
 */
export const add${BusinessName} = (data: ${BusinessName}Form) => {
  return request({
    url: '/${moduleName}/${businessName}',
    method: 'post',
    data: data
  });
};

/**
 * 修改${functionName}
 * @param data
 */
export const update${BusinessName} = (data: ${BusinessName}Form) => {
  return request({
    url: '/${moduleName}/${businessName}',
    method: 'put',
    data: data
  });
};

<#if enableStatus>
/**
 * 修改${functionName}状态
 * @param ${pkColumn.javaField}
 * @param status
 */
export const change${BusinessName}Status = (${pkColumn.javaField}: string | number, status: <#if statusColumn.javaType == 'Boolean'>boolean<#elseif statusColumn.javaType == 'String'>string<#else> number</#if>) => {
  return request({
    url: '/${moduleName}/${businessName}/changeStatus',
    method: 'put',
    data: {
      ${pkColumn.javaField},
      ${statusField}: status
    }
  });
};
</#if>

<#if enableSort>
/**
 * 调整${functionName}排序
 * @param ${pkColumn.javaField}
 * @param sortValue
 */
export const update${BusinessName}Sort = (${pkColumn.javaField}: string | number, sortValue: <#if sortColumn.javaType == 'String' || sortColumn.javaType == 'LocalDateTime'>string<#else> number</#if>) => {
  return request({
    url: '/${moduleName}/${businessName}/updateSort',
    method: 'put',
    data: {
      ${pkColumn.javaField},
      ${sortField}: sortValue
    }
  });
};
</#if>

/**
 * 删除${functionName}
 * @param ${pkColumn.javaField}
 */
export const del${BusinessName} = (${pkColumn.javaField}: string | number | Array<string | number>) => {
  return request({
    url: '/${moduleName}/${businessName}/' + ${pkColumn.javaField},
    method: 'delete'
  });
};
