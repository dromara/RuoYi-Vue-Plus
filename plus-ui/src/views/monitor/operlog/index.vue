<template>
  <div class="p-2 app-container monitor-operlog-page">
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
          <el-form-item label="操作地址" prop="operIp">
            <el-input v-model="queryParams.operIp" placeholder="请输入操作地址" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="系统模块" prop="title">
            <el-input v-model="queryParams.title" placeholder="请输入系统模块" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="操作人员" prop="operName">
            <el-input
              v-model="queryParams.operName"
              placeholder="请输入操作人员"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="客户端" prop="clientKey">
            <el-input v-model="queryParams.clientKey" placeholder="请输入客户端" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="设备类型" prop="deviceType">
            <el-select v-model="queryParams.deviceType" placeholder="请选择设备类型" clearable>
              <el-option v-for="dict in sys_device_type" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="浏览器" prop="browser">
            <el-input v-model="queryParams.browser" placeholder="请输入浏览器" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="操作系统" prop="os">
            <el-input v-model="queryParams.os" placeholder="请输入操作系统" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="类型" prop="businessType">
            <el-select v-model="queryParams.businessType" placeholder="操作类型" clearable>
              <el-option v-for="dict in sys_oper_type" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="操作状态" clearable>
              <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作时间" style="width: 308px">
            <el-date-picker
              v-model="dateRange"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
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
            <span class="panel-kicker">Operation Logs</span>
            <h3>操作日志</h3>
            <p>共 {{ total }} 条记录，支持类型过滤、详情查看、批量清空和导出。</p>
          </div>
          <div class="toolbar-actions">
            <el-button
              v-hasPermi="['monitor:operlog:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除
            </el-button>
            <el-button
              v-hasPermi="['monitor:operlog:remove']"
              type="danger"
              plain
              icon="WarnTriangleFilled"
              @click="handleClean"
            >
              清空
            </el-button>
            <el-button
              v-hasPermi="['monitor:operlog:export']"
              type="warning"
              plain
              icon="Download"
              @click="handleExport"
            >
              导出
            </el-button>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>

      <el-table
        ref="operLogTableRef"
        v-loading="loading"
        :data="operlogList"
        class="data-table"
        border
        :default-sort="defaultSort"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="日志编号" align="center" prop="operId" />
        <el-table-column label="系统模块" align="center" prop="title" :show-overflow-tooltip="true" />
        <el-table-column label="操作类型" align="center" prop="businessType">
          <template #default="scope">
            <dict-tag :options="sys_oper_type" :value="scope.row.businessType" />
          </template>
        </el-table-column>
        <el-table-column
          label="操作人员"
          align="center"
          width="110"
          prop="operName"
          :show-overflow-tooltip="true"
          sortable="custom"
          :sort-orders="['descending', 'ascending']"
        />
        <el-table-column label="部门" align="center" prop="deptName" width="130" :show-overflow-tooltip="true" />
        <el-table-column label="客户端" align="center" prop="clientKey" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="设备类型" align="center" prop="deviceType" width="110" :show-overflow-tooltip="true">
          <template #default="scope">
            <dict-tag :options="sys_device_type" :value="scope.row.deviceType" />
          </template>
        </el-table-column>
        <el-table-column label="浏览器" align="center" prop="browser" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="操作系统" align="center" prop="os" width="110" :show-overflow-tooltip="true" />
        <el-table-column label="操作地址" align="center" prop="operIp" width="130" :show-overflow-tooltip="true" />
        <el-table-column label="操作状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_common_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column
          label="操作日期"
          align="center"
          prop="operTime"
          width="180"
          sortable="custom"
          :sort-orders="['descending', 'ascending']"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.operTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="消耗时间"
          align="center"
          prop="costTime"
          width="110"
          :show-overflow-tooltip="true"
          sortable="custom"
          :sort-orders="['descending', 'ascending']"
        >
          <template #default="scope">
            <span>{{ scope.row.costTime }}毫秒</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="详细" placement="top">
              <el-button
                v-hasPermi="['monitor:operlog:query']"
                link
                type="primary"
                icon="View"
                @click="handleView(scope.row)"
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
    <!-- 操作日志详细 -->
    <OperInfoDialog ref="operInfoDialogRef" />
  </div>
</template>

<script setup name="Operlog" lang="ts">
import { list, delOperlog, cleanOperlog } from '@/api/monitor/operlog';
import { OperLogForm, OperLogQuery, OperLogVO } from '@/api/monitor/operlog/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useDateRangeQuery } from '@/hooks/form/useDateRangeQuery';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useTableSortQuery } from '@/hooks/table/useTableSortQuery';
import modal from '@/plugins/modal';
import { useDict } from '@/utils/dict';
import { download as requestDownload } from '@/utils/request';
import { parseTime, selectDictLabel } from '@/utils/ruoyi';
import OperInfoDialog from './operInfoDialog.vue';

const { sys_oper_type, sys_common_status, sys_device_type } = toRefs<any>(
  useDict('sys_oper_type', 'sys_common_status', 'sys_device_type')
);

const operlogList = ref<OperLogVO[]>([]);
const { loading, withLoading } = useLoading(true);
const { showSearch } = useSearchToggle();
const total = ref(0);
const { dateRange, applyDateRange, resetDateRange } = useDateRangeQuery();

const operLogTableRef = ref<ElTableInstance>();
const queryFormRef = ref<ElFormInstance>();

const data = reactive<PageData<OperLogForm, OperLogQuery>>({
  form: {
    operId: undefined,
    tenantId: undefined,
    title: '',
    businessType: 0,
    businessTypes: undefined,
    method: '',
    requestMethod: '',
    operatorType: 0,
    operName: '',
    userId: undefined,
    deptId: undefined,
    deptName: '',
    clientKey: '',
    deviceType: '',
    browser: '',
    os: '',
    operUrl: '',
    operIp: '',
    operLocation: '',
    operParam: '',
    jsonResult: '',
    status: 0,
    errorMsg: '',
    operTime: '',
    costTime: 0
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    operIp: '',
    title: '',
    operName: '',
    userId: '',
    deptId: '',
    clientKey: '',
    deviceType: '',
    browser: '',
    os: '',
    businessType: '',
    status: '',
    orderByColumn: 'operTime',
    isAsc: 'descending'
  },
  rules: {}
});

const { queryParams, form } = toRefs(data);
const {
  ids,
  multiple,
  handleSelectionChange: handleTableSelectionChange
} = useTableSelection<OperLogVO>(item => item.operId);

/** 查询登录日志 */
const getList = async () => {
  await withLoading(async () => {
    const res = await list(applyDateRange(queryParams.value));
    operlogList.value = res.data?.rows;
    total.value = res.data?.total;
  });
};
const { defaultSort, handleSortChange, resetSort } = useTableSortQuery<OperLogQuery>({
  queryParams,
  tableRef: operLogTableRef,
  defaultSort: { prop: 'operTime', order: 'descending' },
  onSortChange: getList
});
/** 操作日志类型字典翻译 */
const typeFormat = (row: OperLogForm) => {
  return selectDictLabel(sys_oper_type.value, row.businessType);
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  resetExtras: () => {
    resetDateRange();
  },
  afterReset: () => {
    resetSort();
  }
});
const handleSelectionChange = (selection: OperLogVO[]) => {
  handleTableSelectionChange(selection);
};

const operInfoDialogRef = ref<InstanceType<typeof OperInfoDialog>>();
/** 详细按钮操作 */
const handleView = (row: Partial<OperLogVO>) => {
  operInfoDialogRef.value.openDialog(row as OperLogForm);
};

/** 删除按钮操作 */
const handleDelete = async (row?: Partial<OperLogVO>) => {
  const operIds = row?.operId || ids.value;
  await modal.confirm('是否确认删除日志编号为"' + operIds + '"的数据项?');
  await delOperlog(operIds);
  await getList();
  modal.msgSuccess('删除成功');
};

/** 清空按钮操作 */
const handleClean = async () => {
  await modal.confirm('是否确认清空所有操作日志数据项?');
  await cleanOperlog();
  await getList();
  modal.msgSuccess('清空成功');
};

/** 导出按钮操作 */
const handleExport = () => {
  requestDownload(
    'monitor/operlog/export',
    {
      ...queryParams.value
    },
    `operlog_${new Date().getTime()}.xlsx`
  );
};
onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/page-shell' as pageShell;

@include pageShell.table-crud-page;
</style>
