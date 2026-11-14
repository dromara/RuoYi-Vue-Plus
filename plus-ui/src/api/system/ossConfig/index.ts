import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { OssConfigForm, OssConfigQuery, OssConfigVO } from './types';

// 查询对象存储配置列表
export function listOssConfig(query: OssConfigQuery): AxiosPromise<PageResult<OssConfigVO>> {
  return request({
    url: '/resource/oss/config/list',
    method: 'get',
    params: query
  });
}

// 查询对象存储配置详细
export function getOssConfig(ossConfigId: string | number): AxiosPromise<OssConfigVO> {
  return request({
    url: '/resource/oss/config/' + ossConfigId,
    method: 'get'
  });
}

// 新增对象存储配置
export function addOssConfig(data: OssConfigForm) {
  return request({
    url: '/resource/oss/config',
    method: 'post',
    data: data
  });
}

// 修改对象存储配置
export function updateOssConfig(data: OssConfigForm) {
  return request({
    url: '/resource/oss/config',
    method: 'put',
    data: data
  });
}

// 删除对象存储配置
export function delOssConfig(ossConfigId: string | number | Array<string | number>) {
  return request({
    url: '/resource/oss/config/' + ossConfigId,
    method: 'delete'
  });
}

// 对象存储状态修改
export function changeOssConfigStatus(ossConfigId: string | number, status: string, configKey: string) {
  const data = {
    ossConfigId,
    status,
    configKey
  };
  return request({
    url: '/resource/oss/config/changeStatus',
    method: 'put',
    data: data
  });
}
