import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { OssQuery, OssUploadVO, OssVO } from './types';

// 查询OSS对象存储列表
export function listOss(query: OssQuery): AxiosPromise<PageResult<OssVO>> {
  return request({
    url: '/resource/oss/list',
    method: 'get',
    params: query
  });
}

// 查询OSS对象基于id串
export function listByIds(ossId: string | number): AxiosPromise<OssVO[]> {
  return request({
    url: '/resource/oss/listByIds/' + ossId,
    method: 'get'
  });
}

// 上传OSS对象存储
export function uploadOss(data: FormData): AxiosPromise<OssUploadVO> {
  return request({
    url: '/resource/oss/upload',
    method: 'post',
    data
  });
}

// 删除OSS对象存储
export function delOss(ossId: string | number | Array<string | number>) {
  return request({
    url: '/resource/oss/' + ossId,
    method: 'delete'
  });
}
