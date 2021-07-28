import request from '@/utils/request'

// 查询OSS云存储列表
export function listOss(query) {
  return request({
    url: '/system/oss/list',
    method: 'get',
    params: query
  })
}

// 删除OSS云存储
export function delOss(ossId) {
  return request({
    url: '/system/oss/' + ossId,
    method: 'delete'
  })
}
