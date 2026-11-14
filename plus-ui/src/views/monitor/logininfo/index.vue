<template>
  <div class="p-2 app-container monitor-logininfo-page">
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
          <el-form-item label="登录地址" prop="ipaddr">
            <el-input v-model="queryParams.ipaddr" placeholder="请输入登录地址" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="用户名称" prop="userName">
            <el-input
              v-model="queryParams.userName"
              placeholder="请输入用户名称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="登录状态" clearable>
              <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="登录时间" style="width: 308px">
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
            <span class="panel-kicker">Access Logs</span>
            <h3>登录日志</h3>
            <p>共 {{ total }} 条记录，支持排序、批量清理、导出和账号解锁。</p>
          </div>
          <div class="toolbar-actions">
            <el-button
              v-hasPermi="['monitor:logininfo:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除
            </el-button>
            <el-button v-hasPermi="['monitor:logininfo:remove']" type="danger" plain icon="Delete" @click="handleClean">
              清空
            </el-button>
            <el-button
              v-hasPermi="['monitor:logininfo:unlock']"
              type="primary"
              plain
              icon="Unlock"
              :disabled="single"
              @click="handleUnlock"
            >
              解锁
            </el-button>
            <el-button
              v-hasPermi="['monitor:logininfo:export']"
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
        ref="loginInfoTableRef"
        v-loading="loading"
        :data="loginInfoList"
        class="data-table"
        :default-sort="defaultSort"
        border
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="访问编号" align="center" prop="infoId" />
        <el-table-column
          label="用户名称"
          align="center"
          prop="userName"
          :show-overflow-tooltip="true"
          sortable="custom"
          :sort-orders="['descending', 'ascending']"
        />
        <el-table-column label="客户端" align="center" prop="clientKey" :show-overflow-tooltip="true" />
        <el-table-column label="设备类型" align="center">
          <template #default="scope">
            <dict-tag :options="sys_device_type" :value="scope.row.deviceType" />
          </template>
        </el-table-column>
        <el-table-column label="地址" align="center" prop="ipaddr" :show-overflow-tooltip="true" />
        <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true" />
        <el-table-column label="操作系统" align="center" prop="os" :show-overflow-tooltip="true" />
        <el-table-column label="浏览器" align="center" prop="browser" :show-overflow-tooltip="true" />
        <el-table-column label="登录状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_common_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="描述" align="center" prop="msg" :show-overflow-tooltip="true" />
        <el-table-column
          label="访问时间"
          align="center"
          prop="loginTime"
          sortable="custom"
          :sort-orders="['descending', 'ascending']"
          width="180"
        >
          <template #default="scope">
            <span>{{ parseTime(scope.row.loginTime) }}</span>
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
  </div>
</template>

<script setup name="LoginInfo" lang="ts">
import { list, delLoginInfo, cleanLoginInfo, unlockLoginInfo } from '@/api/monitor/logininfo';
import { LoginInfoQuery, LoginInfoVO } from '@/api/monitor/logininfo/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useDateRangeQuery } from '@/hooks/form/useDateRangeQuery';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useTableSortQuery } from '@/hooks/table/useTableSortQuery';
import modal from '@/plugins/modal';
import { useDict } from '@/utils/dict';
import { download as requestDownload } from '@/utils/request';
import { parseTime } from '@/utils/ruoyi';

const { sys_device_type } = toRefs<any>(useDict('sys_device_type'));
const { sys_common_status } = toRefs<any>(useDict('sys_common_status'));

const loginInfoList = ref<LoginInfoVO[]>([]);
const { loading, withLoading } = useLoading(true);
const { showSearch } = useSearchToggle();
const total = ref(0);
const { dateRange, applyDateRange, resetDateRange } = useDateRangeQuery();

const queryFormRef = ref<ElFormInstance>();
const loginInfoTableRef = ref<ElTableInstance>();
// 查询参数
const queryParams = ref<LoginInfoQuery>({
  pageNum: 1,
  pageSize: 10,
  ipaddr: '',
  userName: '',
  status: '',
  orderByColumn: 'loginTime',
  isAsc: 'descending'
});
const {
  ids,
  selectedRows,
  single,
  multiple,
  handleSelectionChange: handleTableSelectionChange
} = useTableSelection<LoginInfoVO>(item => item.infoId);
const selectName = computed(() => selectedRows.value.map(item => item.userName));

/** 查询登录日志列表 */
const getList = async () => {
  await withLoading(async () => {
    const res = await list(applyDateRange(queryParams.value));
    loginInfoList.value = res.data?.rows;
    total.value = res.data?.total;
  });
};
const { defaultSort, handleSortChange, resetSort } = useTableSortQuery<LoginInfoQuery>({
  queryParams,
  tableRef: loginInfoTableRef,
  defaultSort: { prop: 'loginTime', order: 'descending' },
  onSortChange: getList
});
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
const handleSelectionChange = (selection: LoginInfoVO[]) => {
  handleTableSelectionChange(selection);
};
/** 删除按钮操作 */
const handleDelete = async (row?: LoginInfoVO) => {
  const infoIds = row?.infoId || ids.value;
  await modal.confirm('是否确认删除访问编号为"' + infoIds + '"的数据项?');
  await delLoginInfo(infoIds);
  await getList();
  modal.msgSuccess('删除成功');
};
/** 清空按钮操作 */
const handleClean = async () => {
  await modal.confirm('是否确认清空所有登录日志数据项?');
  await cleanLoginInfo();
  await getList();
  modal.msgSuccess('清空成功');
};
/** 解锁按钮操作 */
const handleUnlock = async () => {
  const username = selectName.value;
  await modal.confirm('是否确认解锁用户"' + username + '"数据项?');
  await unlockLoginInfo(username);
  modal.msgSuccess('用户' + username + '解锁成功');
};
/** 导出按钮操作 */
const handleExport = () => {
  requestDownload(
    'monitor/loginInfo/export',
    {
      ...queryParams.value
    },
    `logininfo_${new Date().getTime()}.xlsx`
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
