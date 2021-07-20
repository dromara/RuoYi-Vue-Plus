import axios from 'axios'
import { getToken } from '@/utils/auth'

const mimeMap = {
  oss: 'application/octet-stream'
}

const baseUrl = process.env.VUE_APP_BASE_API
export function downLoadOss(ossId, filename) {
  var url = baseUrl + '/system/oss/download/' + ossId
    axios({
    method: 'get',
    url: url,
    responseType: 'blob',
    headers: { 'Authorization': 'Bearer ' + getToken() }
  }).then(res => {
    resolveBlob(res, mimeMap.oss, filename)
  })
}
/**
 * 解析blob响应内容并下载
 * @param {*} res blob响应内容
 * @param {String} mimeType MIME类型
 */
export function resolveBlob(res, mimeType, filename) {
  const aLink = document.createElement('a')
  var blob = new Blob([res.data], { type: mimeType })
  aLink.href = URL.createObjectURL(blob)
  aLink.setAttribute('download', filename) // 设置下载文件名称
  document.body.appendChild(aLink)
  aLink.click()
  document.body.removeChild(aLink);
}
