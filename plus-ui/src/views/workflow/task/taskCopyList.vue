<template>
  <div class="p-2 app-container workflow-task-copy-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div><h3>筛选条件</h3></div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
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
            <h3>抄送任务</h3>
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
        <el-table-column align="center" prop="version" label="版本号" width="90">
          <template #default="scope">v{{ scope.row.version }}.0</template>
        </el-table-column>
        <el-table-column align="center" prop="nodeName" label="任务名称"></el-table-column>
        <el-table-column align="center" label="流程状态" min-width="70">
          <template #default="scope">
            <dict-tag :options="wf_business_status" :value="scope.row.flowStatus"></dict-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" icon="View" @click="handleView(scope.row)">查看</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { pageByTaskCopy } from '@/api/workflow/task';
import { TaskQuery } from '@/api/workflow/task/types';
import workflowCommon from '@/api/workflow/workflowCommon';
import { RouterJumpVo } from '@/api/workflow/workflowCommon/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useDict } from '@/utils/dict';

const queryFormRef = ref<ElFormInstance>();
const { wf_business_status } = toRefs<any>(useDict('wf_business_status'));
const { loading, withLoading } = useLoading(true);
const { ids, single, multiple, handleSelectionChange } = useTableSelection<any>(item => item.id);
const { showSearch } = useSearchToggle();
// 总条数
const total = ref(0);
// 模型定义表格数据
const taskList = ref([]);
// 查询参数
const queryParams = ref<TaskQuery>({
  pageNum: 1,
  pageSize: 10,
  nodeName: undefined,
  flowName: undefined,
  flowCode: undefined
});
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  pageSizeKey: 'pageSize',
  initialPageSize: 10,
  afterReset: () => {
    handleQuery();
  }
});
/** 搜索按钮操作 */
const handleQuery = () => {
  getTaskCopyList();
};
//分页
const getTaskCopyList = () => {
  withLoading(async () => {
    const resp = await pageByTaskCopy(queryParams.value);
    taskList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
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

onMounted(() => {
  getTaskCopyList();
});
</script>
