import request from '@/utils/request'

// 查询OSS云存储列表
export function listOss(query) {
  return request({
    url: '/system/oss/list',
    method: 'get',
    params: query
  })
}

// 上传OSS云存储
export function addOss(data) {
  return request({
    url: '/system/oss/upload',
    method: 'post',
    data: data
  })
}

// 下载OSS云存储
export function downloadOss(ossId) {
  return request({
    url: '/system/oss/download/' + ossId,
    method: 'get'
  })
}

// 删除OSS云存储
export function delOss(ossId) {
  return request({
    url: '/system/oss/' + ossId,
    method: 'delete'
  })
}
