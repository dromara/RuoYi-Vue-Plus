import { saveAs } from 'file-saver'
import axios from 'axios'
import { getToken } from '@/utils/auth'
import { Message } from 'element-ui'

const baseURL = process.env.VUE_APP_BASE_API

export default {
  excel(url, params) {
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
    url = baseURL + url
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then(async (res) => {
      const isLogin = await this.blobValidate(res.data);
      if (isLogin) {
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        this.saveAs(blob, decodeURI(res.headers['download-filename']))
      } else {
          Message.error('无效的会话，或者会话已过期，请重新登录。');
      }
    })
  },
  oss(ossId) {
    var url = baseURL + '/system/oss/download/' + ossId
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then(async (res) => {
      const isLogin = await this.blobValidate(res.data);
      if (isLogin) {
        const blob = new Blob([res.data], { type: 'application/octet-stream' })
        this.saveAs(blob, decodeURI(res.headers['download-filename']))
      } else {
        Message.error('无效的会话，或者会话已过期，请重新登录。');
      }
    })
  },
  zip(url, name) {
    var url = baseURL + url
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then(async (res) => {
      const isLogin = await this.blobValidate(res.data);
      if (isLogin) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        Message.error('无效的会话，或者会话已过期，请重新登录。');
      }
    })
  },
  saveAs(text, name, opts) {
    saveAs(text, name, opts);
  },
  async blobValidate(data) {
    try {
      const text = await data.text();
      JSON.parse(text);
      return false;
    } catch (error) {
      return true;
    }
  },
}

