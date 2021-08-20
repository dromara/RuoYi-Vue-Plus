import axios from 'axios'
import { getToken } from '@/utils/auth'

const mimeMap = {
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  zip: 'application/zip',
  oss: 'application/octet-stream'
}

const baseUrl = process.env.VUE_APP_BASE_API
export function downLoadZip(str, filename) {
  var url = baseUrl + str
  axios({
    method: 'get',
    url: url,
    responseType: 'blob',
    headers: { 'Authorization': 'Bearer ' + getToken() }
  }).then(res => {
    resolveBlob(res, mimeMap.zip)
  })
}

export function downLoadOss(ossId) {
  var url = baseUrl + '/system/oss/download/' + ossId
  axios({
    method: 'get',
    url: url,
    responseType: 'blob',
    headers: { 'Authorization': 'Bearer ' + getToken() }
  }).then(res => {
    resolveBlob(res, mimeMap.oss)
  })
}

export function downLoadExcel(url, params) {
  // get请求映射params参数
  if (params) {
    let urlparams = url + '?';
    for (const propName of Object.keys(params)) {
      const value = params[propName];
      var part = encodeURIComponent(propName) + "=";
      if (value !== null && typeof(value) !== "undefined") {
        if (typeof value === 'object') {
          for (const key of Object.keys(value)) {
            if (value[key] !== null && typeof (value[key]) !== 'undefined') {
              let params = propName + '[' + key + ']';
              let subPart = encodeURIComponent(params) + '=';
              urlparams += subPart + encodeURIComponent(value[key]) + '&';
            }
          }
        } else {
          urlparams += part + encodeURIComponent(value) + "&";
        }
      }
    }
    urlparams = urlparams.slice(0, -1);
    url = urlparams;
  }
  url = baseUrl + url
  axios({
    method: 'get',
    url: url,
    responseType: 'blob',
    headers: { 'Authorization': 'Bearer ' + getToken() }
  }).then(res => {
    resolveBlob(res, mimeMap.xlsx)
  })
}

/**
 * 解析blob响应内容并下载
 * @param {*} res blob响应内容
 * @param {String} mimeType MIME类型
 */
export function resolveBlob(res, mimeType) {
  const aLink = document.createElement('a')
  var blob = new Blob([res.data], { type: mimeType })
  // //从response的headers中获取filename, 后端response.setHeader("Content-disposition", "attachment; filename=xxxx.docx") 设置的文件名;
  var patt = new RegExp('filename=([^;]+\\.[^\\.;]+);*')
  var contentDisposition = decodeURI(res.headers['content-disposition'])
  var result = patt.exec(contentDisposition)
  var fileName = result[1]
  fileName = fileName.replace(/\"/g, '')
  aLink.style.display = 'none'
  aLink.href = URL.createObjectURL(blob)
  aLink.setAttribute('download', decodeURI(fileName)) // 设置下载文件名称
  document.body.appendChild(aLink)
  aLink.click()
  URL.revokeObjectURL(aLink.href);//清除引用
  document.body.removeChild(aLink);
}
