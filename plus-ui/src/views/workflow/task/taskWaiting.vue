<template>
  <div class="p-2 app-container workflow-task-waiting-page">
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
          <el-form-item label="流程定义编码" label-width="100" prop="flowCode">
            <el-input v-model="queryParams.flowCode" placeholder="请输入流程定义编码" @keyup.enter="handleQuery" />
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
            <h3>待办任务</h3>
          </div>
          <div class="toolbar-actions">
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="handleQuery"></right-toolbar>
          </div>
        </div>
      </template>

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
          label="流程定义名称"
        ></el-table-column>
        <el-table-column align="center" prop="flowCode" label="流程定义编码"></el-table-column>
        <el-table-column align="center" prop="categoryName" label="流程分类"></el-table-column>
        <el-table-column align="center" prop="nodeName" label="任务名称"></el-table-column>
        <el-table-column align="center" prop="createByName" label="申请人"></el-table-column>
        <el-table-column align="center" label="办理人" min-width="180">
          <template #default="scope">
            <UserNameDisplay :content="scope.row.assigneeNames" />
          </template>
        </el-table-column>
        <el-table-column align="center" label="流程状态" prop="flowStatusName" min-width="70">
          <template #default="scope">
            <dict-tag :options="wf_business_status" :value="scope.row.flowStatus"></dict-tag>
          </template>
        </el-table-column>
        <el-table-column align="center" prop="createTime" label="创建时间" width="160"></el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" icon="Edit" @click="handleOpen(scope.row)">办理</el-button>
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
    </el-card>
    <!-- 申请人 -->
    <UserSelect
      ref="userSelectRef"
      :multiple="true"
      :data="selectUserIds"
      @confirm-call-back="userSelectCallBack"
    ></UserSelect>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { UserVO } from '@/api/system/user/types';
import { pageByTaskWait } from '@/api/workflow/task';
import { TaskQuery, FlowTaskVO } from '@/api/workflow/task/types';
import workflowCommon from '@/api/workflow/workflowCommon';
import { RouterJumpVo } from '@/api/workflow/workflowCommon/types';
import UserNameDisplay from '@/components/Process/UserNameDisplay.vue';
import UserSelect from '@/components/UserSelect/index.vue';
import { useLoading } from '@/hooks/async/useLoading';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useDict } from '@/utils/dict';

const { wf_business_status } = toRefs<any>(useDict('wf_business_status'));

const userSelectRef = ref<InstanceType<typeof UserSelect>>();
//提交组件
const queryFormRef = ref<ElFormInstance>();
const { loading, withLoading } = useLoading(true);
const { ids, single, multiple, handleSelectionChange } = useTableSelection<any>(item => item.id);
const { showSearch } = useSearchToggle();
// 总条数
const total = ref(0);
// 模型定义表格数据
const taskList = ref([]);

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
onMounted(() => {
  getWaitingList();
});
/** 搜索按钮操作 */
const handleQuery = () => {
  getWaitingList();
};
//分页
const getWaitingList = () => {
  withLoading(async () => {
    const resp = await pageByTaskWait(queryParams.value);
    taskList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};
//办理
const handleOpen = async (row: Partial<FlowTaskVO>) => {
  const routerJumpVo = reactive<RouterJumpVo>({
    businessId: row.businessId,
    taskId: row.id,
    type: 'approval',
    formCustom: row.formCustom,
    formPath: row.formPath
  });
  workflowCommon.routerJump(routerJumpVo);
};
//打开申请人选择
const openUserSelect = () => {
  userSelectRef.value.open();
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
</script>
