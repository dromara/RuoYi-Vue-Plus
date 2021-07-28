import request from '@/utils/request'

// 查询测试单表列表
export function listDemo(query) {
  return request({
    url: '/demo/demo/list',
    method: 'get',
    params: query
  })
}

// 自定义分页接口
export function pageDemo(query) {
  return request({
    url: '/demo/demo/page',
    method: 'get',
    params: query
  })
}

// 查询测试单表详细
export function getDemo(id) {
  return request({
    url: '/demo/demo/' + id,
    method: 'get'
  })
}

// 新增测试单表
export function addDemo(data) {
  return request({
    url: '/demo/demo',
    method: 'post',
    data: data
  })
}

// 修改测试单表
export function updateDemo(data) {
  return request({
    url: '/demo/demo',
    method: 'put',
    data: data
  })
}

// 删除测试单表
export function delDemo(id) {
  return request({
    url: '/demo/demo/' + id,
    method: 'delete'
  })
}

// 导出测试单表
export function exportDemo(query) {
  return request({
    url: '/demo/demo/export',
    method: 'get',
    params: query
  })
}
