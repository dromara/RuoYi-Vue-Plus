import request from '@/utils/request'

// 查询云存储配置列表
export function listSysOssConfig(query) {
  return request({
    url: '/system/sysOssConfig/list',
    method: 'get',
    params: query
  })
}

// 查询云存储配置详细
export function getSysOssConfig(ossConfigId) {
  return request({
    url: '/system/sysOssConfig/' + ossConfigId,
    method: 'get'
  })
}

// 新增云存储配置
export function addSysOssConfig(data) {
  return request({
    url: '/system/sysOssConfig',
    method: 'post',
    data: data
  })
}

// 修改云存储配置
export function updateSysOssConfig(data) {
  return request({
    url: '/system/sysOssConfig',
    method: 'put',
    data: data
  })
}

// 删除云存储配置
export function delSysOssConfig(ossConfigId) {
  return request({
    url: '/system/sysOssConfig/' + ossConfigId,
    method: 'delete'
  })
}

// 用户状态修改
export function changeOssConfigStatus(ossConfigId, status,configKey) {
  const data = {
    ossConfigId,
    status,
    configKey
  }
  return request({
    url: '/system/sysOssConfig/changeStatus',
    method: 'put',
    data: data
  })
}
