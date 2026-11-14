<template>
  <div class="p-2 app-container tool-gen-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div>
              <span class="panel-kicker">Search Filters</span>
              <h3>筛选条件</h3>
            </div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
          <el-form-item label="数据源" prop="dataName">
            <el-select v-model="queryParams.dataName" filterable clearable placeholder="请选择/输入数据源名称">
              <el-option key="" label="全部" value="" />
              <el-option v-for="item in dataNameList" :key="item" :label="item" :value="item"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="表名称" prop="tableName">
            <el-input v-model="queryParams.tableName" placeholder="请输入表名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="表描述" prop="tableComment">
            <el-input
              v-model="queryParams.tableComment"
              placeholder="请输入表描述"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="创建时间" style="width: 308px">
            <el-date-picker
              v-model="dateRange"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            ></el-date-picker>
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
            <span class="panel-kicker">Code Generator</span>
            <h3>数据表列表</h3>
            <p>共 {{ total }} 条记录，支持导入表结构、同步数据库和代码预览生成。</p>
          </div>
          <div class="toolbar-actions">
            <el-button v-hasPermi="['tool:gen:code']" type="primary" plain icon="Download" @click="handleGenTable()">
              生成
            </el-button>
            <el-button v-hasPermi="['tool:gen:import']" type="info" plain icon="Upload" @click="openImportTable">
              导入
            </el-button>
            <el-button
              v-hasPermi="['tool:gen:edit']"
              type="success"
              plain
              icon="Edit"
              :disabled="single"
              @click="handleEditTable()"
            >
              修改
            </el-button>
            <el-button
              v-hasPermi="['tool:gen:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除
            </el-button>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        border
        class="data-table"
        :data="tableList"
        @selection-change="handleTableSelectionChange"
      >
        <el-table-column type="selection" align="center" width="55"></el-table-column>
        <el-table-column label="序号" type="index" width="50" align="center">
          <template #default="scope">
            <span>{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据源" align="center" prop="dataName" :show-overflow-tooltip="true" />
        <el-table-column label="表名称" align="center" prop="tableName" :show-overflow-tooltip="true" />
        <el-table-column label="表描述" align="center" prop="tableComment" :show-overflow-tooltip="true" />
        <el-table-column label="实体" align="center" prop="className" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
        <el-table-column label="更新时间" align="center" prop="updateTime" width="160" />
        <el-table-column label="操作" align="center" width="330" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="预览" placement="top">
              <el-button
                v-hasPermi="['tool:gen:preview']"
                link
                type="primary"
                icon="View"
                @click="handlePreview(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button
                v-hasPermi="['tool:gen:edit']"
                link
                type="primary"
                icon="Edit"
                @click="handleEditTable(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button
                v-hasPermi="['tool:gen:remove']"
                link
                type="primary"
                icon="Delete"
                @click="handleDelete(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="同步" placement="top">
              <el-button
                v-hasPermi="['tool:gen:edit']"
                link
                type="primary"
                icon="Refresh"
                @click="handleSynchDb(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="生成代码" placement="top">
              <el-button
                v-hasPermi="['tool:gen:code']"
                link
                type="primary"
                icon="Download"
                @click="handleGenTable(scope.row)"
              ></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 预览界面 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="80%" top="5vh" append-to-body class="scrollbar">
      <el-tabs v-model="preview.activeName">
        <el-tab-pane
          v-for="(value, key) in preview.data"
          :key="value"
          :label="key.substring(key.lastIndexOf('/') + 1, key.indexOf('.ftl'))"
          :name="key.substring(key.lastIndexOf('/') + 1, key.indexOf('.ftl'))"
        >
          <el-link
            v-copyText="value"
            v-copyText:callback="copyTextSuccess"
            underline="never"
            icon="DocumentCopy"
            style="float: right"
          >
            &nbsp;复制
          </el-link>
          <highlightjs :code="value" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
    <import-table ref="importRef" @ok="handleQuery" />
  </div>
</template>

<script setup name="Gen" lang="ts">
import { useRoute } from 'vue-router';
import { delTable, getDataNames, listTable, previewTable, synchDb } from '@/api/tool/gen';
import { TableQuery, TableVO } from '@/api/tool/gen/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useDialogState } from '@/hooks/dialog/useDialogState';
import { useDateRangeQuery } from '@/hooks/form/useDateRangeQuery';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import download from '@/plugins/download';
import modal from '@/plugins/modal';
import router from '@/router';
import ImportTable from './importTable.vue';

const route = useRoute();

const tableList = ref<TableVO[]>([]);
const { loading, withLoading } = useLoading(true);
const { showSearch } = useSearchToggle();
const total = ref(0);
const { dateRange, applyDateRange, resetDateRange } = useDateRangeQuery();
const uniqueId = ref('');
const dataNameList = ref<Array<string>>([]);

const queryFormRef = ref<ElFormInstance>();
const importRef = ref<InstanceType<typeof ImportTable>>();
const selectedRows = ref<TableVO[]>([]);

const queryParams = ref<TableQuery>({
  pageNum: 1,
  pageSize: 10,
  tableName: '',
  tableComment: '',
  dataName: ''
});

const preview = ref<{
  data: Record<string, string>;
  activeName: string;
}>({
  data: {},
  activeName: 'domain.java'
});
const {
  ids,
  single,
  multiple,
  handleSelectionChange: updateSelection
} = useTableSelection<TableVO>(item => item.tableId);
const { dialog, openDialog: openPreviewDialog } = useDialogState('代码预览');

const handleTableSelectionChange = (selection: TableVO[]) => {
  selectedRows.value = selection;
  updateSelection(selection);
};

/** 查询多数据源名称 */
const getDataNameList = async () => {
  const res = await getDataNames();
  dataNameList.value = res.data;
};

/** 查询表集合 */
const getList = async () => {
  await withLoading(async () => {
    const res = await listTable(applyDateRange(queryParams.value));
    tableList.value = res.data?.rows;
    total.value = res.data?.total;
  });
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
/** 生成代码操作 */
const handleGenTable = async (row?: Partial<TableVO>) => {
  const currentRows = row ? [row] : selectedRows.value;
  if (!currentRows.length) {
    modal.msgError('请选择要生成的数据');
    return;
  }

  const zipIds = currentRows.map(item => item.tableId).join(',');
  download.zip('/tool/gen/batchGenCode?tableIdStr=' + zipIds, 'ruoyi.zip');
};
/** 同步数据库操作 */
const handleSynchDb = async (row: Partial<TableVO>) => {
  const tableId = row.tableId;
  await modal.confirm('确认要强制同步"' + row.tableName + '"表结构吗？');
  await synchDb(tableId);
  modal.msgSuccess('同步成功');
};
/** 打开导入表弹窗 */
const openImportTable = () => {
  importRef.value?.show(queryParams.value.dataName);
};
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  resetExtras: () => {
    resetDateRange();
  },
  afterReset: () => {
    handleQuery();
  }
});
/** 预览按钮 */
const handlePreview = async (row: Partial<TableVO>) => {
  const res = await previewTable(row.tableId);
  preview.value.data = res.data;
  openPreviewDialog();
  preview.value.activeName = 'domain.java';
};
/** 复制代码成功 */
const copyTextSuccess = () => {
  modal.msgSuccess('复制成功');
};
/** 修改按钮操作 */
const handleEditTable = (row?: Partial<TableVO>) => {
  const tableId = row?.tableId || ids.value[0];
  router.push({
    path: '/tool/gen-edit/index/' + tableId,
    query: { pageNum: queryParams.value.pageNum }
  });
};
/** 删除按钮操作 */
const handleDelete = async (row?: Partial<TableVO>) => {
  const tableIds = row?.tableId || ids.value;
  await modal.confirm('是否确认删除表编号为"' + tableIds + '"的数据项？');
  await delTable(tableIds);
  await getList();
  modal.msgSuccess('删除成功');
};

onMounted(() => {
  const time = route.query.t;
  if (time != null && time != uniqueId.value) {
    uniqueId.value = time as string;
    queryParams.value.pageNum = Number(route.query.pageNum);
    resetDateRange();
    queryFormRef.value?.resetFields();
  }
  getList();
  getDataNameList();
});
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/page-shell' as pageShell;

@include pageShell.table-crud-page;

.el-tab-pane {
  background-color: #282c34;
  .el-link {
    color: #fff;
  }
}
</style>
