import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { MessageBoxVO } from './types';

export function getMessageBox(): AxiosPromise<MessageBoxVO> {
  return request({
    url: '/resource/message/box',
    method: 'get'
  });
}
