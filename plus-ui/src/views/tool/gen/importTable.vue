<template>
  <el-dialog v-model="visible" title="导入表" width="1100px" top="5vh" append-to-body>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true">
      <el-form-item label="数据源" prop="dataName">
        <el-select v-model="queryParams.dataName" filterable placeholder="请选择/输入数据源名称">
          <el-option v-for="item in dataNameList" :key="item" :label="item" :value="item"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="表名称" prop="tableName">
        <el-input v-model="queryParams.tableName" placeholder="请输入表名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="表描述" prop="tableComment">
        <el-input v-model="queryParams.tableComment" placeholder="请输入表描述" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row>
      <el-table
        ref="tableRef"
        border
        :data="dbTableList"
        height="260px"
        @row-click="clickRow"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="tableName" label="表名称" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="tableComment" label="表描述" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="createTime" label="创建时间"></el-table-column>
        <el-table-column prop="updateTime" label="更新时间"></el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-row>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleImportTable">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { listDbTable, importTable, getDataNames } from '@/api/tool/gen';
import { DbTableQuery, DbTableVO } from '@/api/tool/gen/types';
import modal from '@/plugins/modal';

const total = ref(0);
const visible = ref(false);
const tables = ref<Array<string>>([]);
const dbTableList = ref<Array<DbTableVO>>([]);

const tableRef = ref<ElTableInstance>();
const queryFormRef = ref<ElFormInstance>();

const queryParams = reactive<DbTableQuery>({
  pageNum: 1,
  pageSize: 10,
  dataName: '',
  tableName: '',
  tableComment: ''
});
const dataNameList = ref<Array<string>>([]);

const emit = defineEmits(['ok']);

/** 查询参数列表 */
const show = (dataName: string) => {
  getDataNames().then(res => {
    if (res.code == 200) {
      dataNameList.value = res.data;
      if (dataName) {
        queryParams.dataName = dataName;
      } else {
        queryParams.dataName = dataNameList.value[0];
      }
      getList();
      visible.value = true;
    }
  });
};
/** 单击选择行 */
const clickRow = (row: DbTableVO) => {
  // ele bug
  tableRef.value?.toggleRowSelection(row, false);
};
/** 多选框选中数据 */
const handleSelectionChange = (selection: DbTableVO[]) => {
  tables.value = selection.map(item => item.tableName);
};
/** 查询表数据 */
const getList = async () => {
  const res = await listDbTable(queryParams);
  dbTableList.value = res.data?.rows;
  total.value = res.data?.total;
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};
/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};
/** 导入按钮操作 */
const handleImportTable = async () => {
  const tableNames = tables.value.join(',');
  if (tableNames == '') {
    modal.msgError('请选择要导入的表');
    return;
  }
  const res = await importTable({
    tables: tableNames,
    dataName: queryParams.dataName
  });
  modal.msgSuccess(res.msg ?? '');
  if (res.code === 200) {
    visible.value = false;
    emit('ok');
  }
};

defineExpose({
  show
});
</script>
