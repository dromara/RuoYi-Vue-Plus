<template>
  <div class="p-2 app-container workflow-all-task-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div><h3>筛选条件</h3></div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
          <el-form-item>
            <el-badge :value="userSelectCount" :max="10" class="item">
              <el-button type="primary" @click="openUserSelect">选择申请人</el-button>
            </el-badge>
          </el-form-item>
          <el-form-item label="任务名称" prop="nodeName">
            <el-input v-model="queryParams.nodeName" placeholder="请输入任务名称" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="流程定义名称" label-width="100" prop="flowName">
            <el-input v-model="queryParams.flowName" placeholder="请输入流程定义名称" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>全部任务</h3>
          </div>
          <div class="toolbar-actions">
            <template v-if="tab === 'waiting'">
              <el-button
                class="todo-action-btn todo-action-btn--primary"
                type="primary"
                plain
                icon="Edit"
                :disabled="multiple"
                @click="handleUserOpen()"
              >
                修改办理人
              </el-button>
              <el-button
                class="todo-action-btn todo-action-btn--warning"
                type="warning"
                plain
                icon="Bell"
                :disabled="multiple"
                @click="handleUrgeTaskOpen()"
              >
                催办
              </el-button>
            </template>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="handleQuery"></right-toolbar>
          </div>
        </div>
      </template>
      <el-tabs v-model="tab" @tab-click="changeTab">
        <el-tab-pane name="waiting" label="待办任务"></el-tab-pane>
        <el-tab-pane name="finish" label="已办任务"></el-tab-pane>
        <el-table
          v-loading="loading"
          border
          class="data-table"
          :data="taskList"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column align="center" type="index" label="序号" width="60"></el-table-column>
          <el-table-column
            :show-overflow-tooltip="true"
            prop="businessCode"
            align="center"
            label="业务编码"
          ></el-table-column>
          <el-table-column
            :show-overflow-tooltip="true"
            prop="businessTitle"
            align="center"
            label="业务标题"
          ></el-table-column>
          <el-table-column
            :show-overflow-tooltip="true"
            prop="flowName"
            align="center"
            width="120"
            label="流程定义名称"
          ></el-table-column>
          <el-table-column align="center" prop="flowCode" width="120" label="流程定义编码"></el-table-column>
          <el-table-column align="center" prop="categoryName" label="流程分类"></el-table-column>
          <el-table-column align="center" prop="version" label="版本号" width="90">
            <template #default="scope">v{{ scope.row.version }}.0</template>
          </el-table-column>
          <el-table-column
            align="center"
            prop="nodeName"
            :show-overflow-tooltip="true"
            label="任务名称"
          ></el-table-column>
          <el-table-column
            align="center"
            prop="createByName"
            :show-overflow-tooltip="true"
            label="申请人"
          ></el-table-column>
          <el-table-column align="center" label="办理人" min-width="180">
            <template #default="scope">
              <template v-if="tab === 'waiting'">
                <UserNameDisplay :content="scope.row.assigneeNames" />
              </template>
              <template v-else>
                <UserNameDisplay :content="scope.row.approverName" />
              </template>
            </template>
          </el-table-column>
          <el-table-column align="center" label="流程状态" prop="flowStatus" min-width="70">
            <template #default="scope">
              <dict-tag :options="wf_business_status" :value="scope.row.flowStatus"></dict-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="tab === 'finish'" align="center" label="任务状态" prop="flowTaskStatus" min-width="70">
            <template #default="scope">
              <dict-tag :options="wf_task_status" :value="scope.row.flowTaskStatus"></dict-tag>
            </template>
          </el-table-column>
          <el-table-column align="center" prop="createTime" label="创建时间" width="160"></el-table-column>
          <el-table-column label="操作" align="center" :width="tab === 'finish' ? '88' : '188'">
            <template #default="scope">
              <el-row :gutter="10" class="mb8">
                <el-col :span="1.5" v-if="tab === 'waiting' || tab === 'finish'">
                  <el-button type="primary" size="small" icon="View" @click="handleView(scope.row)">查看</el-button>
                </el-col>
                <el-col :span="1.5" v-if="tab === 'waiting'">
                  <el-button type="primary" size="small" icon="Setting" @click="handleMeddle(scope.row)">
                    流程干预
                  </el-button>
                </el-col>
              </el-row>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="total > 0"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :total="total"
          @pagination="handleQuery"
        />
      </el-tabs>
    </el-card>
    <!-- 选人组件 -->
    <UserSelect ref="userSelectRef" :multiple="userMultiple" @confirm-call-back="submitCallback"></UserSelect>
    <!-- 流程干预组件 -->
    <processMeddle ref="processMeddleRef" @submitCallback="getWaitingList"></processMeddle>
    <!-- 申请人 -->
    <UserSelect
      ref="applyUserSelectRef"
      :multiple="true"
      :data="selectUserIds"
      @confirm-call-back="userSelectCallBack"
    ></UserSelect>
    <!-- 流程干预组件 -->
    <messageType ref="messageTypeRef" @submitCallback="handleUserTask"></messageType>
  </div>
</template>

<script setup lang="ts">
import { TabsPaneContext } from 'element-plus';
import { UserVO } from '@/api/system/user/types';
import { pageByAllTaskWait, pageByAllTaskFinish, updateAssignee, urgeTask } from '@/api/workflow/task';
import { TaskQuery, FlowTaskVO } from '@/api/workflow/task/types';
import workflowCommon from '@/api/workflow/workflowCommon';
import { RouterJumpVo } from '@/api/workflow/workflowCommon/types';
import messageType from '@/components/Process/MessageType.vue';
import processMeddle from '@/components/Process/processMeddle.vue';
import UserNameDisplay from '@/components/Process/UserNameDisplay.vue';
import UserSelect from '@/components/UserSelect/index.vue';
import { useLoading } from '@/hooks/async/useLoading';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import modal from '@/plugins/modal';
import { useDict } from '@/utils/dict';

//选人组件
const userSelectRef = ref<InstanceType<typeof UserSelect>>();
//流程干预组件
const processMeddleRef = ref<InstanceType<typeof processMeddle>>();
//选人组件
const applyUserSelectRef = ref<InstanceType<typeof UserSelect>>();
//消息组件
const messageTypeRef = ref<InstanceType<typeof messageType>>();
const queryFormRef = ref<ElFormInstance>();
const { wf_business_status } = toRefs<any>(useDict('wf_business_status'));
const { wf_task_status } = toRefs<any>(useDict('wf_task_status'));
const { loading, withLoading } = useLoading(true);
const { ids, single, multiple, handleSelectionChange } = useTableSelection<FlowTaskVO, string>(item => String(item.id));
const userMultiple = ref(false);
const { showSearch } = useSearchToggle();
// 总条数
const total = ref(0);
// 模型定义表格数据
const taskList = ref([]);
const buttonType = ref('');
//申请人id
const selectUserIds = ref<Array<number | string>>([]);
//申请人选择数量
const userSelectCount = ref(0);
// 查询参数
const queryParams = ref<TaskQuery>({
  pageNum: 1,
  pageSize: 10,
  nodeName: undefined,
  flowName: undefined,
  flowCode: undefined,
  createByIds: []
});
const tab = ref('waiting');
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  pageSizeKey: 'pageSize',
  initialPageSize: 10,
  resetExtras: () => {
    queryParams.value.createByIds = [];
    userSelectCount.value = 0;
    selectUserIds.value = [];
  },
  afterReset: () => {
    handleQuery();
  }
});

/** 搜索按钮操作 */
const handleQuery = () => {
  if ('waiting' === tab.value) {
    getWaitingList();
  } else {
    getFinishList();
  }
};
const changeTab = async (data: TabsPaneContext) => {
  taskList.value = [];
  queryParams.value.pageNum = 1;
  if ('waiting' === data.paneName) {
    getWaitingList();
  } else {
    getFinishList();
  }
};
//分页
const getWaitingList = () => {
  withLoading(async () => {
    const resp = await pageByAllTaskWait(queryParams.value);
    taskList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};
const getFinishList = () => {
  withLoading(async () => {
    const resp = await pageByAllTaskFinish(queryParams.value);
    taskList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};
// 打开催办
const handleUrgeTaskOpen = () => {
  messageTypeRef.value.open();
};
//打开修改选人
const handleUserOpen = () => {
  userSelectRef.value.open();
};

//打开修改选人
const handleUserTask = async data => {
  await modal.confirm('是否确认提交？');
  data.taskIdList = ids.value;
  await urgeTask(data);
  messageTypeRef.value.close();
  modal.msgSuccess('操作成功');
  handleQuery();
};
//修改办理人
const submitCallback = async data => {
  if (data && data.length > 0) {
    await modal.confirm('是否确认提交？');
    await updateAssignee(ids.value, data[0].userId);
    handleQuery();
    modal.msgSuccess('操作成功');
  } else {
    modal.msgWarning('请选择用户！');
  }
};
/** 查看按钮操作 */
const handleView = row => {
  const routerJumpVo = reactive<RouterJumpVo>({
    businessId: row.businessId,
    taskId: row.id,
    type: 'view',
    formCustom: row.formCustom,
    formPath: row.formPath
  });
  workflowCommon.routerJump(routerJumpVo);
};
const handleMeddle = row => {
  processMeddleRef.value.open(row.id);
};
//打开申请人选择
const openUserSelect = () => {
  applyUserSelectRef.value.open();
};
//确认选择申请人
const userSelectCallBack = (data: UserVO[]) => {
  userSelectCount.value = 0;
  selectUserIds.value = [];
  queryParams.value.createByIds = [];

  if (data && data.length > 0) {
    userSelectCount.value = data.length;
    selectUserIds.value = data.map(item => item.userId);
    queryParams.value.createByIds = selectUserIds.value;
  }
};
onMounted(() => {
  getWaitingList();
});
</script>

<style lang="scss" scoped>
.todo-action-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: var(--app-radius-md);
  font-weight: 600;
  --el-button-bg-color: var(--el-fill-color-lighter);
  --el-button-border-color: var(--el-border-color-light);
  --el-button-text-color: var(--el-text-color-regular);
  --el-button-hover-bg-color: var(--el-fill-color-light);
  --el-button-hover-border-color: var(--el-border-color);
}

.todo-action-btn--primary {
  --el-button-text-color: var(--el-color-primary);
  --el-button-border-color: var(--el-color-primary-light-5);
  --el-button-bg-color: var(--el-color-primary-light-9);
  --el-button-hover-bg-color: var(--el-color-primary-light-8);
  --el-button-hover-border-color: var(--el-color-primary-light-5);
}

.todo-action-btn--warning {
  --el-button-text-color: var(--el-color-warning);
  --el-button-border-color: var(--el-color-warning-light-5);
  --el-button-bg-color: var(--el-color-warning-light-9);
  --el-button-hover-bg-color: var(--el-color-warning-light-8);
  --el-button-hover-border-color: var(--el-color-warning-light-5);
}

html.dark {
  .todo-action-btn {
    --el-button-bg-color: rgba(148, 163, 184, 0.12);
    --el-button-border-color: rgba(148, 163, 184, 0.3);
    --el-button-text-color: var(--el-text-color-regular);
    --el-button-hover-bg-color: rgba(148, 163, 184, 0.2);
    --el-button-hover-border-color: rgba(148, 163, 184, 0.45);
  }

  .todo-action-btn--primary {
    --el-button-bg-color: rgba(99, 113, 150, 0.14);
    --el-button-border-color: rgba(99, 113, 150, 0.45);
    --el-button-text-color: #c4cfdd;
    --el-button-hover-bg-color: rgba(99, 113, 150, 0.22);
    --el-button-hover-border-color: rgba(99, 113, 150, 0.65);
  }

  .todo-action-btn--warning {
    --el-button-bg-color: rgba(214, 149, 59, 0.16);
    --el-button-border-color: rgba(214, 149, 59, 0.55);
    --el-button-text-color: #ffe6bb;
    --el-button-hover-bg-color: rgba(214, 149, 59, 0.24);
    --el-button-hover-border-color: rgba(214, 149, 59, 0.75);
  }
}
</style>
