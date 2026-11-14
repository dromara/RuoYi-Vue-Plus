import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { SnailOpenApiUser } from './types';

export const registerCurrentSnailUser = (): AxiosPromise<SnailOpenApiUser> => {
  return request({
    url: '/snail-ai/user/register',
    method: 'post'
  });
};
