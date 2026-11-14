import type { RouterJumpVo } from '@/api/workflow/workflowCommon/types';
import tab from '@/plugins/tab';
import router from '@/router';

export default {
  routerJump(routerJumpVo: RouterJumpVo) {
    tab.closePage(router.currentRoute.value);
    router.push({
      path: routerJumpVo.formPath,
      query: {
        id: routerJumpVo.businessId,
        instanceId: routerJumpVo.instanceId,
        type: routerJumpVo.type,
        taskId: routerJumpVo.taskId
      }
    });
  }
};
