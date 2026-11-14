<template>
  <div class="p-2 app-container workflow-process-definition-page">
    <el-row :gutter="20" class="content-grid">
      <!-- 流程分类树 -->
      <tree-panel
        ref="treePanelRef"
        v-model:collapsed="treeCollapsed"
        title="流程分类"
        placeholder="请输入流程分类名"
        :data="categoryOptions"
        :expanded-span="4"
        filter-field="categoryName"
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
            <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="120px" class="query-form">
              <el-form-item label="流程定义名称" prop="flowName">
                <el-input
                  v-model="queryParams.flowName"
                  placeholder="请输入流程定义名称"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
              <el-form-item label="流程定义编码" prop="flowCode">
                <el-input
                  v-model="queryParams.flowCode"
                  placeholder="请输入流程定义编码"
                  clearable
                  @keyup.enter="handleQuery"
                />
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
                <h3>流程定义</h3>
              </div>
              <div class="toolbar-actions">
                <el-button v-hasPermi="['workflow:definition:add']" type="primary" icon="Plus" @click="handleAdd()">
                  添加
                </el-button>
                <el-button
                  v-hasPermi="['workflow:definition:remove']"
                  type="danger"
                  icon="Delete"
                  :disabled="multiple"
                  @click="handleDelete()"
                >
                  删除
                </el-button>
                <el-button
                  v-hasPermi="['workflow:definition:import']"
                  type="primary"
                  icon="UploadFilled"
                  @click="openUploadDialog()"
                >
                  部署流程文件
                </el-button>
                <el-button
                  v-hasPermi="['workflow:definition:export']"
                  type="warning"
                  icon="Download"
                  :disabled="single"
                  @click="handleExportDef"
                >
                  导出
                </el-button>
                <right-toolbar
                  v-model:show-search="showSearch"
                  :search="false"
                  @query-table="handleQuery"
                ></right-toolbar>
              </div>
            </div>
          </template>
          <el-tabs v-model="activeName" class="demo-tabs" @tab-click="handleClick">
            <el-tab-pane label="已发布" name="0"></el-tab-pane>
            <el-tab-pane label="未发布" name="1"></el-tab-pane>
            <el-table
              v-loading="loading"
              border
              class="data-table"
              :data="processDefinitionList"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="55" align="center" />
              <el-table-column align="center" prop="id" label="主键" v-if="false"></el-table-column>
              <el-table-column
                align="center"
                prop="flowName"
                label="流程定义名称"
                :show-overflow-tooltip="true"
              ></el-table-column>
              <el-table-column
                align="center"
                prop="flowCode"
                label="标识KEY"
                :show-overflow-tooltip="true"
              ></el-table-column>
              <el-table-column
                align="center"
                prop="categoryName"
                label="流程分类"
                :show-overflow-tooltip="true"
              ></el-table-column>
              <el-table-column align="center" prop="version" label="版本号" width="80">
                <template #default="scope">v{{ scope.row.version }}.0</template>
              </el-table-column>
              <el-table-column align="center" prop="activityStatus" label="激活状态" width="130">
                <template #default="scope">
                  <el-switch
                    v-hasPermi="['workflow:definition:active']"
                    v-model="scope.row.activityStatus"
                    :active-value="1"
                    :inactive-value="0"
                    @change="status => handleProcessDefState(scope.row, status)"
                  />
                </template>
              </el-table-column>
              <el-table-column align="center" prop="isPublish" label="发布状态" width="100">
                <template #default="scope">
                  <el-tag v-if="scope.row.isPublish == 0" type="danger">未发布</el-tag>
                  <el-tag v-else-if="scope.row.isPublish == 1" type="success">已发布</el-tag>
                  <el-tag v-else type="danger">失效</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                fixed="right"
                label="操作"
                align="center"
                width="236"
                class-name="small-padding fixed-width"
              >
                <template #default="scope">
                  <div class="process-action-group">
                    <el-button
                      v-hasPermi="['workflow:definition:remove']"
                      link
                      type="primary"
                      size="small"
                      icon="Delete"
                      @click="handleDelete(scope.row)"
                    >
                      删除流程
                    </el-button>
                    <el-button
                      v-hasPermi="['workflow:definition:copy']"
                      link
                      type="primary"
                      size="small"
                      icon="CopyDocument"
                      @click="handleCopyDef(scope.row)"
                    >
                      复制流程
                    </el-button>
                    <el-button
                      v-hasPermi="['workflow:definition:query']"
                      link
                      type="primary"
                      v-if="scope.row.isPublish === 0"
                      icon="Pointer"
                      size="small"
                      @click="design(scope.row)"
                    >
                      流程设计
                    </el-button>
                    <el-button
                      v-hasPermi="['workflow:definition:query']"
                      link
                      type="primary"
                      v-else
                      icon="View"
                      size="small"
                      @click="designView(scope.row)"
                    >
                      查看流程
                    </el-button>
                    <el-button
                      v-hasPermi="['workflow:definition:publish']"
                      link
                      type="primary"
                      v-if="scope.row.isPublish !== 1"
                      size="small"
                      icon="CircleCheck"
                      @click="handlePublish(scope.row)"
                    >
                      发布流程
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <pagination
              v-show="total > 0"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
              :total="total"
              @pagination="getPageList"
            />
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <!-- 部署文件 -->
    <el-dialog v-if="uploadDialog.visible" v-model="uploadDialog.visible" :title="uploadDialog.title" width="30%">
      <div v-loading="uploadDialogLoading">
        <div class="mb5">
          <el-text class="mx-1" size="large">
            <span class="text-danger">*</span>
            请选择部署流程分类：
          </el-text>
          <el-tree-select
            v-model="selectCategory"
            :data="categoryOptions"
            :props="{ value: 'id', label: 'label', children: 'children' } as any"
            filterable
            value-key="id"
            :render-after-expand="false"
            check-strictly
            style="width: 240px"
          />
        </div>
        <el-upload
          class="upload-demo"
          drag
          multiple
          accept="application/json,application/text"
          :before-upload="handlerBeforeUpload"
          :http-request="handlerImportDefinition"
        >
          <el-icon class="UploadFilled"><upload-filled /></el-icon>
          <div class="el-upload__text"><em>点击上传，选择JSON流程文件</em></div>
          <div class="el-upload__text">仅支持json格式文件</div>
          <div class="el-upload__text">PS:如若部署请部署从本项目模型管理导出的数据</div>
        </el-upload>
      </div>
    </el-dialog>

    <!-- 新增/编辑流程定义 -->
    <el-dialog
      v-model="modelDialog.visible"
      :title="modelDialog.title"
      width="650px"
      append-to-body
      :close-on-click-modal="false"
      class="definition-dialog"
    >
      <el-form ref="defFormRef" :model="form" :rules="rules" label-width="120px" class="definition-form">
        <el-form-item label="流程类别" prop="category">
          <el-tree-select
            v-model="form.category"
            :data="categoryOptions"
            :props="{ value: 'id', label: 'label', children: 'children' } as any"
            filterable
            value-key="id"
            :render-after-expand="false"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="流程编码" prop="flowCode">
          <el-input v-model="form.flowCode" placeholder="请输入流程编码" maxlength="40" show-word-limit />
        </el-form-item>
        <el-form-item label="流程名称" prop="flowName">
          <el-input v-model="form.flowName" placeholder="请输入流程名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="设计器模式" prop="modelValue">
          <el-radio-group v-model="form.modelValue" :disabled="!!form.id" class="definition-radio-group">
            <el-radio value="CLASSICS" size="large" border>经典模式</el-radio>
            <el-radio value="MIMIC" size="large" border>仿钉钉模式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否动态表单" prop="formCustom">
          <el-radio-group v-model="form.formCustom" class="definition-radio-group">
            <el-radio value="Y" size="large" border disabled>是</el-radio>
            <el-radio value="N" size="large" border>否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="表单路径" prop="formPath">
          <el-input v-model="form.formPath" placeholder="请输入表单路径" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeModelDialog()">取消</el-button>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="processDefinition" lang="ts">
import type { ElMessageBoxOptions, TabsPaneContext, UploadRequestOptions } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { categoryTree } from '@/api/workflow/category';
import { CategoryTreeVO } from '@/api/workflow/category/types';
import {
  listDefinition,
  deleteDefinition,
  active,
  importDef,
  unPublishList,
  publish,
  add,
  copy
} from '@/api/workflow/definition';
import { FlowDefinitionQuery, FlowDefinitionVo, FlowDefinitionForm } from '@/api/workflow/definition/types';
import TreePanel from '@/components/TreePanel/index.vue';
import { useLoading } from '@/hooks/async/useLoading';
import { useDialogState } from '@/hooks/dialog/useDialogState';
import { useFormDialog } from '@/hooks/dialog/useFormDialog';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useTreeCollapsed } from '@/hooks/tree/useTreeCollapsed';
import modal from '@/plugins/modal';
import { download as requestDownload } from '@/utils/request';

const route = useRoute();
const router = useRouter();

const queryFormRef = ref<ElFormInstance>();
const treePanelRef = ref<InstanceType<typeof TreePanel>>();

const { loading, setLoading, withLoading } = useLoading(true);
const total = ref(0);
const uploadDialogLoading = ref(false);
const processDefinitionList = ref<FlowDefinitionVo[]>([]);
const categoryOptions = ref<CategoryTreeVO[]>([]);
const { treeCollapsed } = useTreeCollapsed();
const { showSearch } = useSearchToggle();
/** 部署文件分类选择 */
const selectCategory = ref();
const defFormRef = ref<ElFormInstance>();
const activeName = ref('0');

// 查询参数
const queryParams = ref<FlowDefinitionQuery>({
  pageNum: 1,
  pageSize: 10,
  flowName: undefined,
  flowCode: undefined,
  category: undefined
});
const rules = {
  category: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
  flowName: [{ required: true, message: '流程定义名称不能为空', trigger: 'blur' }],
  formCustom: [{ required: true, message: '请选择是否动态表单', trigger: 'change' }],
  modelValue: [{ required: true, message: '设计器模式不能为空', trigger: 'change' }],
  flowCode: [{ required: true, message: '流程定义编码不能为空', trigger: 'blur' }]
};
const initFormData: FlowDefinitionForm = {
  id: '',
  flowName: '',
  flowCode: '',
  category: '',
  ext: '',
  formPath: '',
  formCustom: '',
  modelValue: ''
};
//流程定义参数
const form = ref<FlowDefinitionForm>({
  id: '',
  flowName: '',
  flowCode: '',
  category: '',
  ext: '',
  formPath: '',
  formCustom: '',
  modelValue: ''
});
const { ids, selectedRows, single, multiple, handleSelectionChange } = useTableSelection<FlowDefinitionVo, string>(
  item => String(item.id)
);
const flowCodeList = computed(() => selectedRows.value.map(item => item.flowCode));
const {
  dialog: uploadDialog,
  openDialog: openUploadDialog,
  closeDialog: closeUploadDialog
} = useDialogState('部署流程文件');
const {
  dialog: modelDialog,
  resetForm: reset,
  showDialog: showModelDialog,
  closeDialog: closeModelDialog
} = useFormDialog({
  form,
  formRef: defFormRef,
  initialFormData: initFormData
});
onMounted(() => {
  getPageList();
  getTreeselect();
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
const handleClick = (tab: TabsPaneContext, event: Event) => {
  // v-model处理有延迟 需要手动处理
  activeName.value = tab.index;
  handleQuery();
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  if (activeName.value === '0') {
    getList();
  } else {
    getUnPublishList();
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
  },
  afterReset: () => {
    handleQuery();
  }
});
//分页
const getPageList = async () => {
  if (route.query.activeName) {
    activeName.value = route.query.activeName as string;
    const q = { ...route.query };
    delete q.activeName;
    router.replace({ path: route.path, query: q });
  }
  if (activeName.value === '0') {
    getList();
  } else {
    getUnPublishList();
  }
};
//分页
const getList = async () => {
  await withLoading(async () => {
    const resp = await listDefinition(queryParams.value);
    processDefinitionList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};
//查询未发布的流程定义列表
const getUnPublishList = async () => {
  await withLoading(async () => {
    const resp = await unPublishList(queryParams.value);
    processDefinitionList.value = resp.data?.rows;
    total.value = resp.data?.total;
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: Partial<FlowDefinitionVo>) => {
  const id = row?.id || ids.value;
  const defList = processDefinitionList.value.filter(x => id.indexOf(x.id) != -1).map(x => x.flowCode);
  await modal.confirm('是否确认删除流程定义编码为【' + defList + '】的数据项？');
  setLoading(true);
  await deleteDefinition(id).finally(() => setLoading(false));
  await handleQuery();
  modal.msgSuccess('删除成功');
};

/** 发布流程定义 */
const handlePublish = async (row?: Partial<FlowDefinitionVo>) => {
  await modal.confirm(
    '是否确认发布流程定义编码为【' +
      row.flowCode +
      '】版本为【' +
      row.version +
      '】的数据项？，发布后会将已发布流程定义改为失效！'
  );
  setLoading(true);
  await publish(row.id).finally(() => setLoading(false));
  activeName.value = '0';
  await handleQuery();
  modal.msgSuccess('发布成功');
};
/** 挂起/激活 */
const handleProcessDefState = async (row: Partial<FlowDefinitionVo>, status: number | string | boolean) => {
  let msg: string;
  if (status === 0) {
    msg = `暂停后，此流程下的所有任务都不允许往后流转，您确定挂起【${row.flowName || row.flowCode}】吗？`;
  } else {
    msg = `启动后，此流程下的所有任务都允许往后流转，您确定激活【${row.flowName || row.flowCode}】吗？`;
  }
  try {
    setLoading(true);
    await modal.confirm(msg);
    await active(row.id, !!status);
    await handleQuery();
    modal.msgSuccess('操作成功');
  } catch (error) {
    row.activityStatus = status === 0 ? 1 : 0;
    console.error(error);
  } finally {
    setLoading(false);
  }
};

//上传文件前的钩子
const handlerBeforeUpload = () => {
  if (selectCategory.value === 'ALL') {
    modal.msgError('顶级节点不可作为分类！');
    return false;
  }
  if (!selectCategory.value) {
    modal.msgError('请选择左侧要上传的分类！');
    return false;
  }
};
//部署文件
const handlerImportDefinition = (data: UploadRequestOptions): XMLHttpRequest => {
  const formData = new FormData();
  uploadDialogLoading.value = true;
  formData.append('file', data.file);
  formData.append('category', selectCategory.value);
  importDef(formData)
    .then(() => {
      closeUploadDialog();
      modal.msgSuccess('部署成功');
      activeName.value = '1';
      handleQuery();
    })
    .finally(() => {
      uploadDialogLoading.value = false;
    });
  return;
};
/**
 * 设计流程
 * @param row
 */
const design = async (row: Partial<FlowDefinitionVo>) => {
  router.push({
    path: `/workflow/design/index`,
    query: {
      definitionId: String(row.id),
      disabled: 'false',
      activeName: String(activeName.value)
    }
  });
};

/**
 * 查看流程
 * @param row
 */
const designView = async (row: Partial<FlowDefinitionVo>) => {
  router.push({
    path: `/workflow/design/index`,
    query: {
      definitionId: String(row.id),
      disabled: 'true',
      activeName: String(activeName.value)
    }
  });
};
/**
 * 新增
 */
const handleAdd = async () => {
  reset();
  if (queryParams.value.category != null && queryParams.value.category !== '') {
    form.value.category = String(queryParams.value.category);
  }
  form.value.modelValue = 'CLASSICS';
  form.value.formCustom = 'N';
  showModelDialog('新增流程');
};

const handleSubmit = async () => {
  defFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      setLoading(true);
      await add(form.value).finally(() => setLoading(false));
      activeName.value = '1';
      modal.msgSuccess('操作成功');
      closeModelDialog();
      handleQuery();
    }
  });
};
//复制
const handleCopyDef = async (row: Partial<FlowDefinitionVo>) => {
  ElMessageBox.confirm(`是否确认复制【${row.flowCode}】版本为【${row.version}】的流程定义！`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  } as ElMessageBoxOptions).then(() => {
    setLoading(true);
    copy(row.id)
      .then(resp => {
        if (resp.code === 200) {
          modal.msgSuccess('操作成功');
          activeName.value = '1';
          handleQuery();
        }
      })
      .finally(() => setLoading(false));
  });
};

/** 导出按钮操作 */
const handleExportDef = () => {
  requestDownload(`/workflow/definition/exportDef/${ids.value[0]}`, {}, `${flowCodeList.value[0]}.json`);
};
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/page-shell' as pageShell;

@include pageShell.tree-table-crud-page;

.content-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.process-action-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  padding: 2px 0;
}

.process-action-group :deep(.el-button.is-link) {
  width: auto !important;
  min-width: 0 !important;
  height: 30px !important;
  padding: 0 10px !important;
  border-radius: 10px !important;
  background: rgba(53, 109, 255, 0.08) !important;
}

.process-action-group :deep(.el-button.is-link + .el-button.is-link) {
  margin-left: 0 !important;
}

.process-action-group :deep(.el-button .el-icon) {
  margin-right: 4px;
}

.definition-form {
  padding-top: 8px;
}

.definition-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.definition-radio-group :deep(.el-radio) {
  margin-right: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
