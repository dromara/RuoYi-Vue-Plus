import { parseTime } from '@/utils/ruoyi';

/**
 * 表格时间格式化
 */
export const formatDate = (cellValue: string) => {
  if (cellValue == null || cellValue == '') return '';
  const date = new Date(cellValue);
  const year = date.getFullYear();
  const month = date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1;
  const day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
  const hours = date.getHours() < 10 ? '0' + date.getHours() : date.getHours();
  const minutes = date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes();
  const seconds = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds();
  return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
};

/**
 * @param {number} time
 * @param {string} option
 * @returns {string}
 */
export const formatTime = (time: string, option: string) => {
  let t: number;
  if (('' + time).length === 10) {
    t = parseInt(time) * 1000;
  } else {
    t = +time;
  }
  const d: any = new Date(t);
  const now = Date.now();

  const diff = (now - d) / 1000;

  if (diff < 30) {
    return '刚刚';
  } else if (diff < 3600) {
    // less 1 hour
    return Math.ceil(diff / 60) + '分钟前';
  } else if (diff < 3600 * 24) {
    return Math.ceil(diff / 3600) + '小时前';
  } else if (diff < 3600 * 24 * 2) {
    return '1天前';
  }
  if (option) {
    return parseTime(t, option);
  } else {
    return d.getMonth() + 1 + '月' + d.getDate() + '日' + d.getHours() + '时' + d.getMinutes() + '分';
  }
};

/**
 * @param {string} url
 * @returns {Object}
 */
export const getQueryObject = (url: string) => {
  url = url == null ? window.location.href : url;
  const search = url.substring(url.lastIndexOf('?') + 1);
  const obj: { [key: string]: string } = {};
  const reg = /([^?&=]+)=([^?&=]*)/g;
  search.replace(reg, (rs, $1, $2) => {
    const name = decodeURIComponent($1);
    let val = decodeURIComponent($2);
    val = String(val);
    obj[name] = val;
    return rs;
  });
  return obj;
};

/**
 * @param {string} input value
 * @returns {number} output value
 */
export const byteLength = (str: string) => {
  // returns the byte length of an utf8 string
  let s = str.length;
  for (let i = str.length - 1; i >= 0; i--) {
    const code = str.charCodeAt(i);
    if (code > 0x7f && code <= 0x7ff) s++;
    else if (code > 0x7ff && code <= 0xffff) s += 2;
    if (code >= 0xdc00 && code <= 0xdfff) i--;
  }
  return s;
};

/**
 * @param {Array} actual
 * @returns {Array}
 */
export const cleanArray = (actual: Array<any>) => {
  const newArray: any[] = [];
  for (let i = 0; i < actual.length; i++) {
    if (actual[i]) {
      newArray.push(actual[i]);
    }
  }
  return newArray;
};

/**
 * @param {Object} json
 * @returns {Array}
 */
export const param = (json: any) => {
  if (!json) return '';
  return cleanArray(
    Object.keys(json).map(key => {
      if (json[key] === undefined) return '';
      return encodeURIComponent(key) + '=' + encodeURIComponent(json[key]);
    })
  ).join('&');
};

/**
 * @param {string} url
 * @returns {Object}
 */
export const param2Obj = (url: string) => {
  const search = decodeURIComponent(url.split('?')[1]).replace(/\+/g, ' ');
  if (!search) {
    return {};
  }
  const obj: any = {};
  const searchArr = search.split('&');
  searchArr.forEach(v => {
    const index = v.indexOf('=');
    if (index !== -1) {
      const name = v.substring(0, index);
      const val = v.substring(index + 1, v.length);
      obj[name] = val;
    }
  });
  return obj;
};

/**
 * @param {string} val
 * @returns {string}
 */
export const html2Text = (val: string) => {
  const div = document.createElement('div');
  div.innerHTML = val;
  return div.textContent || div.innerText;
};

/**
 * Merges two objects, giving the last one precedence
 * @param {Object} target
 * @param {(Object|Array)} source
 * @returns {Object}
 */
export const objectMerge = (target: any, source: any | any[]) => {
  if (typeof target !== 'object') {
    target = {};
  }
  if (Array.isArray(source)) {
    return source.slice();
  }
  Object.keys(source).forEach(property => {
    const sourceProperty = source[property];
    if (typeof sourceProperty === 'object') {
      target[property] = objectMerge(target[property], sourceProperty);
    } else {
      target[property] = sourceProperty;
    }
  });
  return target;
};

/**
 * @param {HTMLElement} element
 * @param {string} className
 */
export const toggleClass = (element: HTMLElement, className: string) => {
  if (!element || !className) {
    return;
  }
  let classString = element.className;
  const nameIndex = classString.indexOf(className);
  if (nameIndex === -1) {
    classString += '' + className;
  } else {
    classString = classString.substring(0, nameIndex) + classString.substring(nameIndex + className.length);
  }
  element.className = classString;
};

/**
 * @param {string} type
 * @returns {Date}
 */
export const getTime = (type: string) => {
  if (type === 'start') {
    return new Date().getTime() - 3600 * 1000 * 24 * 90;
  } else {
    return new Date(new Date().toDateString());
  }
};

/**
 * @param {Function} func
 * @param {number} wait
 * @param {boolean} immediate
 * @return {*}
 */
export function debounce(func: (...args: any[]) => any, wait: number, immediate: boolean) {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  let lastArgs: any[] | null = null;
  let lastContext: any = null;
  let timestamp = 0;
  let result: any;

  function later() {
    const last = Date.now() - timestamp;

    if (last < wait && last > 0) {
      timeout = setTimeout(later, wait - last);
    } else {
      timeout = null;
      if (!immediate) {
        result = func.apply(lastContext, lastArgs!);
        if (!timeout) lastContext = lastArgs = null;
      }
    }
  }

  return function (this: any, ...args: any[]) {
    lastContext = this;
    lastArgs = args;
    timestamp = Date.now();
    const callNow = immediate && !timeout;
    if (!timeout) timeout = setTimeout(later, wait);
    if (callNow) {
      result = func.apply(lastContext, lastArgs);
      lastContext = lastArgs = null;
    }
    return result;
  };
}

/**
 * This is just a simple version of deep copy
 * Has a lot of edge cases bug
 * If you want to use a perfect deep copy, use lodash's _.cloneDeep
 * @param {Object} source
 * @returns {Object}
 */
export const deepClone = (source: any) => {
  if (!source && typeof source !== 'object') {
    throw new Error('error arguments', 'deepClone' as any);
  }
  const targetObj: any = source.constructor === Array ? [] : {};
  Object.keys(source).forEach(keys => {
    if (source[keys] && typeof source[keys] === 'object') {
      targetObj[keys] = deepClone(source[keys]);
    } else {
      targetObj[keys] = source[keys];
    }
  });
  return targetObj;
};

/**
 * @param {Array} arr
 * @returns {Array}
 */
export const uniqueArr = (arr: any) => {
  return Array.from(new Set(arr));
};

/**
 * @returns {string}
 */
export const createUniqueString = (): string => {
  const timestamp = +new Date() + '';
  const num = (1 + Math.random()) * 65536;
  const randomNum = parseInt(num + '');
  return (+(randomNum + timestamp)).toString(32);
};

/**
 * Check if an element has a class
 * @param ele
 * @param {string} cls
 * @returns {boolean}
 */
export const hasClass = (ele: HTMLElement, cls: string): boolean => {
  return !!ele.className.match(new RegExp('(\\s|^)' + cls + '(\\s|$)'));
};

/**
 * Add class to element
 * @param ele
 * @param {string} cls
 */
export const addClass = (ele: HTMLElement, cls: string) => {
  if (!hasClass(ele, cls)) ele.className += ' ' + cls;
};

/**
 * Remove class from element
 * @param ele
 * @param {string} cls
 */
export const removeClass = (ele: HTMLElement, cls: string) => {
  if (hasClass(ele, cls)) {
    const reg = new RegExp('(\\s|^)' + cls + '(\\s|$)');
    ele.className = ele.className.replace(reg, ' ');
  }
};

/**
 * @param {string} path
 * @returns {Boolean}
 */
export const isExternal = (path: string) => {
  return /^(https?:|http?:|mailto:|tel:)/.test(path);
};
