<template>
  <div class="p-2 app-container workflow-process-instance-page">
    <el-row :gutter="20" class="content-grid">
      <!-- 流程分类树 -->
      <tree-panel
        ref="treePanelRef"
        v-model:collapsed="treeCollapsed"
        title="流程分类"
        placeholder="请输入流程分类名"
        :data="categoryOptions"
        :expanded-span="4"
        filter-field="label"
        @node-click="handleNodeClick"
      />
      <el-col
        :lg="treeCollapsed ? 23 : 20"
        :xs="24"
        class="tree-content-col content-main"
        :class="{ 'is-tree-collapsed': treeCollapsed }"
      >
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
                <h3>流程实例</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  v-hasPermi="['workflow:instance:remove']"
                  type="danger"
                  plain
                  icon="Delete"
                  :disabled="multiple"
                  @click="() => handleDelete()"
                >
                  删除
                </el-button>
                <right-toolbar
                  v-model:show-search="showSearch"
                  :search="false"
                  @query-table="handleQuery"
                ></right-toolbar>
              </div>
            </div>
          </template>
          <el-tabs v-model="tab" @tab-click="changeTab">
            <el-tab-pane name="running" label="运行中"></el-tab-pane>
            <el-tab-pane name="finish" label="已完成"></el-tab-pane>
            <el-table
              v-loading="loading"
              border
              class="data-table"
              :data="processInstanceList"
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
              <el-table-column :show-overflow-tooltip="true" align="center" width="120" label="流程定义名称">
                <template #default="scope">
                  <span>{{ scope.row.flowName }}v{{ scope.row.version }}</span>
                </template>
              </el-table-column>
              <el-table-column align="center" prop="flowCode" width="120" label="流程定义编码"></el-table-column>
              <el-table-column align="center" prop="categoryName" label="流程分类"></el-table-column>
              <el-table-column align="center" prop="nodeName" label="任务名称"></el-table-column>
              <el-table-column
                align="center"
                prop="createByName"
                :show-overflow-tooltip="true"
                label="申请人"
              ></el-table-column>
              <el-table-column align="center" prop="version" label="版本号" width="90">
                <template #default="scope">v{{ scope.row.version }}.0</template>
              </el-table-column>
              <el-table-column
                v-if="tab === 'running'"
                v-hasPermi="['workflow:instance:active']"
                align="center"
                prop="isSuspended"
                label="状态"
                min-width="70"
              >
                <template #default="scope">
                  <el-tag v-if="!scope.row.isSuspended" type="success">激活</el-tag>
                  <el-tag v-else type="danger">挂起</el-tag>
                </template>
              </el-table-column>
              <el-table-column align="center" label="流程状态" min-width="80">
                <template #default="scope">
                  <dict-tag :options="wf_business_status" :value="scope.row.flowStatus"></dict-tag>
                </template>
              </el-table-column>
              <el-table-column align="center" prop="createTime" label="启动时间" width="160"></el-table-column>
              <el-table-column
                v-if="tab === 'finish'"
                align="center"
                prop="updateTime"
                label="结束时间"
                width="160"
              ></el-table-column>
              <el-table-column label="操作" align="center" :width="165" fixed="right">
                <template #default="scope">
                  <el-row v-if="tab === 'running'" :gutter="10" class="mb8">
                    <el-col :span="1.5">
                      <el-popover
                        :ref="el => setPopoverRef(el, scope.$index)"
                        trigger="click"
                        placement="left"
                        :width="300"
                      >
                        <el-input
                          v-model="deleteReason"
                          resize="none"
                          :rows="3"
                          type="textarea"
                          placeholder="请输入作废原因"
                        />
                        <div style="text-align: right; margin: 5px 0px 0px 0px">
                          <el-button size="small" text @click="cancelPopover(scope.$index)">取消</el-button>
                          <el-button size="small" type="primary" @click="handleInvalid(scope.row)">确认</el-button>
                        </div>
                        <template #reference>
                          <el-button type="danger" size="small" icon="CircleClose">作废</el-button>
                        </template>
                      </el-popover>
                    </el-col>
                    <el-col :span="1.5">
                      <el-button
                        v-hasPermi="['workflow:instance:remove']"
                        type="danger"
                        size="small"
                        icon="Delete"
                        @click="handleDelete(scope.row)"
                      >
                        删除
                      </el-button>
                    </el-col>
                  </el-row>
                  <el-row :gutter="10" class="mb8">
                    <el-col :span="1.5">
                      <el-button
                        v-hasPermi="['workflow:instance:query']"
                        type="primary"
                        size="small"
                        icon="View"
                        @click="handleView(scope.row)"
                      >
                        查看
                      </el-button>
                    </el-col>
                    <el-col :span="1.5">
                      <el-button
                        v-hasPermi="['workflow:instance:variableQuery']"
                        type="primary"
                        size="small"
                        icon="Document"
                        @click="handleInstanceVariable(scope.row)"
                      >
                        变量
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
      </el-col>
    </el-row>
    <el-dialog
      v-if="processDefinitionDialog.visible"
      v-model="processDefinitionDialog.visible"
      :title="processDefinitionDialog.title"
      width="70%"
    >
      <el-table v-loading="loading" :data="processDefinitionHistoryList">
        <el-table-column fixed align="center" type="index" label="序号" width="60"></el-table-column>
        <el-table-column fixed align="center" prop="name" label="流程定义名称"></el-table-column>
        <el-table-column fixed align="center" prop="nodeName" label="任务名称"></el-table-column>
        <el-table-column align="center" prop="key" label="标识Key"></el-table-column>
        <el-table-column align="center" prop="version" label="版本号" width="90">
          <template #default="scope">v{{ scope.row.version }}.0</template>
        </el-table-column>
        <el-table-column
          v-hasPermi="['workflow:instance:active']"
          align="center"
          prop="suspensionState"
          label="状态"
          min-width="70"
        >
          <template #default="scope">
            <el-tag v-if="scope.row.suspensionState == 1" type="success">激活</el-tag>
            <el-tag v-else type="danger">挂起</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          align="center"
          prop="deploymentTime"
          label="部署时间"
          :show-overflow-tooltip="true"
        ></el-table-column>
      </el-table>
    </el-dialog>
    <!-- 流程变量开始 -->
    <el-dialog
      v-model="variableVisible"
      v-if="variableVisible"
      draggable
      title="流程变量"
      width="60%"
      :close-on-click-modal="false"
    >
      <el-card v-loading="variableLoading">
        <template #header>
          <div class="clearfix">
            <span>
              流程定义名称：
              <el-tag>{{ processDefinitionName }}</el-tag>
            </span>
          </div>
        </template>
        <div class="max-h-500px overflow-y-auto">
          <VueJsonPretty :data="formatToJsonObject(variables)" />
        </div>
      </el-card>
      <el-card v-loading="variableLoading">
        <el-form ref="ruleFormRef" :model="form" :inline="true" :rules="rules" label-width="120px">
          <el-form-item label="变量KEY" prop="key">
            <el-input v-model="form.key" placeholder="请输入变量KEY" />
          </el-form-item>
          <el-form-item label="变量值" prop="value">
            <el-input v-model="form.value" placeholder="请输入变量值" />
          </el-form-item>
          <el-form-item>
            <el-button v-hasPermi="['workflow:instance:variable']" type="primary" @click="handleVariable(ruleFormRef)">
              确认
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-dialog>
    <!-- 流程变量结束 -->

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
import { ElForm, FormInstance, type TabsPaneContext } from 'element-plus';
import { ref } from 'vue';
import VueJsonPretty from 'vue-json-pretty';
import { UserVO } from '@/api/system/user/types';
import { categoryTree } from '@/api/workflow/category';
import { CategoryTreeVO } from '@/api/workflow/category/types';
import {
  pageByRunning,
  pageByFinish,
  deleteByInstanceIds,
  deleteHisByInstanceIds,
  instanceVariable,
  invalid,
  updateVariable
} from '@/api/workflow/instance';
import { FlowInstanceQuery, FlowInstanceVO } from '@/api/workflow/instance/types';
import 'vue-json-pretty/lib/styles.css';
import workflowCommon from '@/api/workflow/workflowCommon';
import { RouterJumpVo } from '@/api/workflow/workflowCommon/types';
import TreePanel from '@/components/TreePanel/index.vue';
import UserSelect from '@/components/UserSelect/index.vue';
import { useLoading } from '@/hooks/async/useLoading';
import { useDialogState } from '@/hooks/dialog/useDialogState';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useTreeCollapsed } from '@/hooks/tree/useTreeCollapsed';
import modal from '@/plugins/modal';
import { useDict } from '@/utils/dict';

const { wf_business_status } = toRefs<any>(useDict('wf_business_status'));
const queryFormRef = ref<ElFormInstance>();
const treePanelRef = ref<InstanceType<typeof TreePanel>>();
const popoverRefs = ref<Record<number, { hide?: () => void } | null>>({});
const setPopoverRef = (el: unknown, index: number) => {
  if (el) {
    popoverRefs.value[index] = el as { hide?: () => void };
  }
};
const form = ref<Record<string, any>>({
  instanceId: undefined,
  key: undefined,
  value: undefined
});
const ruleFormRef = ref<FormInstance>();
const userSelectRef = ref<InstanceType<typeof UserSelect>>();
const { loading, setLoading, withLoading } = useLoading(true);
const {
  ids: instanceIds,
  single,
  multiple,
  handleSelectionChange
} = useTableSelection<FlowInstanceVO>(item => item.id);
const { showSearch } = useSearchToggle();
// 总条数
const total = ref(0);
// 实例id
const instanceId = ref(undefined);

// 流程变量是否显示
const variableVisible = ref(false);
const variableLoading = ref(true);
const variables = ref<string>('');
//流程定义名称
const processDefinitionName = ref();
// 模型定义表格数据
const processInstanceList = ref<FlowInstanceVO[]>([]);
const processDefinitionHistoryList = ref<Array<any>>([]);
const categoryOptions = ref<CategoryTreeVO[]>([]);
const { treeCollapsed } = useTreeCollapsed();

const { dialog: processDefinitionDialog } = useDialogState('流程定义');

const tab = ref('running');
// 作废原因
const deleteReason = ref('');

//申请人id
const selectUserIds = ref<Array<number | string>>([]);
//申请人选择数量
const userSelectCount = ref(0);
// 查询参数
const queryParams = ref<FlowInstanceQuery>({
  pageNum: 1,
  pageSize: 10,
  nodeName: undefined,
  flowName: undefined,
  flowCode: undefined,
  createByIds: [],
  category: undefined
});

/** 节点单击事件 */
const handleNodeClick = (data: CategoryTreeVO) => {
  queryParams.value.category = data.id;
  if (data.id === '0') {
    queryParams.value.category = '';
  }
  handleQuery();
};
/** 查询流程分类下拉树结构 */
const getTreeselect = async () => {
  const res = await categoryTree();
  categoryOptions.value = res.data;
};

/** 搜索按钮操作 */
const handleQuery = () => {
  if ('running' === tab.value) {
    getProcessInstanceRunningList();
  } else {
    getProcessInstanceFinishList();
  }
};
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  pageSizeKey: 'pageSize',
  initialPageSize: 10,
  resetExtras: () => {
    queryParams.value.category = '';
    queryParams.value.createByIds = [];
    userSelectCount.value = 0;
    selectUserIds.value = [];
  },
  afterReset: () => {
    handleQuery();
  }
});
//分页
const getProcessInstanceRunningList = () => {
  withLoading(async () => {
    const resp = await pageByRunning(queryParams.value);
    processInstanceList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};
//分页
const getProcessInstanceFinishList = () => {
  withLoading(async () => {
    const resp = await pageByFinish(queryParams.value);
    processInstanceList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: Partial<FlowInstanceVO>) => {
  const instanceIdList = row?.id ?? instanceIds.value;
  await modal.confirm('是否确认删除？');
  setLoading(true);
  if ('running' === tab.value) {
    await deleteByInstanceIds(instanceIdList).finally(() => setLoading(false));
    getProcessInstanceRunningList();
  } else {
    await deleteHisByInstanceIds(instanceIdList).finally(() => setLoading(false));
    getProcessInstanceFinishList();
  }
  modal.msgSuccess('删除成功');
};
const changeTab = async (pane: TabsPaneContext) => {
  processInstanceList.value = [];
  queryParams.value.pageNum = 1;
  if ('running' === pane.paneName) {
    getProcessInstanceRunningList();
  } else {
    getProcessInstanceFinishList();
  }
};
/** 作废按钮操作 */
const handleInvalid = async (row: Partial<FlowInstanceVO>) => {
  await modal.confirm('是否确认作废？');
  setLoading(true);
  if ('running' === tab.value) {
    const param = {
      id: row.id,
      comment: deleteReason.value
    };
    await invalid(param).finally(() => setLoading(false));
    getProcessInstanceRunningList();
    modal.msgSuccess('操作成功');
  }
};
const cancelPopover = async (index: number) => {
  popoverRefs.value[index]?.hide?.();
};
/** 查看按钮操作 */
const handleView = row => {
  const routerJumpVo = reactive<RouterJumpVo>({
    businessId: row.businessId,
    instanceId: row.id,
    taskId: row.id,
    type: 'view',
    formCustom: row.formCustom,
    formPath: row.formPath
  });
  workflowCommon.routerJump(routerJumpVo);
};

//查询流程变量
const handleInstanceVariable = async (row: Partial<FlowInstanceVO>) => {
  instanceId.value = row.id;
  variableLoading.value = true;
  variableVisible.value = true;
  processDefinitionName.value = row.flowName;
  const data = await instanceVariable(row.id);
  variables.value = data.data.variable;
  variableLoading.value = false;
  form.value.instanceId = undefined;
  form.value.key = undefined;
  form.value.value = undefined;
};

/**
 * json转为对象
 * @param data 原始数据
 */
function formatToJsonObject(data: string) {
  try {
    return JSON.parse(data);
  } catch (error) {
    return data;
  }
}

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
const rules = reactive<Record<string, any>>({
  key: [
    {
      required: true,
      message: '请输入KEY',
      trigger: 'blur'
    }
  ],
  value: [
    {
      required: true,
      message: '请输入变量值',
      trigger: 'blur'
    }
  ]
});

const handleVariable = async (formEl: FormInstance | undefined) => {
  await formEl.validate(async (valid, fields) => {
    if (valid) {
      form.value.instanceId = instanceId.value;
      await modal.confirm('是否确认提交？');
      await updateVariable(form.value);
      modal.msgSuccess('操作成功');
      const data = await instanceVariable(instanceId.value);
      variables.value = data.data.variable;
    }
  });
};

onMounted(() => {
  getProcessInstanceRunningList();
  getTreeselect();
});
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/page-shell' as pageShell;

@include pageShell.tree-table-crud-page;

.content-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
