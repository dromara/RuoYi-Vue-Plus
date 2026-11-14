<template>
  <el-row>
    <el-dialog v-model="visible" title="选择用户" width="800px" top="5vh" append-to-body class="select-user-dialog">
      <div class="p-2 select-user-shell">
        <el-card shadow="hover" class="search-panel select-user-card">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
            <el-form-item label="用户名称" prop="userName">
              <el-input
                v-model="queryParams.userName"
                placeholder="请输入用户名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="手机号码" prop="phoneNumber">
              <el-input
                v-model="queryParams.phoneNumber"
                placeholder="请输入手机号码"
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

        <el-card shadow="hover" class="table-panel select-user-card">
          <el-table
            ref="tableRef"
            border
            class="data-table"
            :data="userList"
            height="260px"
            @row-click="clickRow"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column label="用户名称" prop="userName" :show-overflow-tooltip="true" />
            <el-table-column label="用户昵称" prop="nickName" :show-overflow-tooltip="true" />
            <el-table-column label="邮箱" prop="email" :show-overflow-tooltip="true" />
            <el-table-column label="手机" prop="phoneNumber" :show-overflow-tooltip="true" />
            <el-table-column label="状态" align="center" prop="status">
              <template #default="scope">
                <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" width="180">
              <template #default="scope">
                <span>{{ parseTime(scope.row.createTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-if="total > 0"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :total="total"
            @pagination="getList"
          />
        </el-card>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleSelectUser">确 定</el-button>
          <el-button @click="visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </el-row>
</template>

<script setup name="SelectUser" lang="ts">
import { authUserSelectAll, unallocatedUserList } from '@/api/system/role';
import { UserVO } from '@/api/system/user/types';
import { UserQuery } from '@/api/system/user/types';
import modal from '@/plugins/modal';
import { useDict } from '@/utils/dict';
import { parseTime } from '@/utils/ruoyi';

const props = defineProps({
  roleId: {
    type: [Number, String],
    required: true
  }
});

const { sys_normal_disable } = toRefs<any>(useDict('sys_normal_disable'));

const userList = ref<UserVO[]>([]);
const visible = ref(false);
const total = ref(0);
const userIds = ref<Array<string | number>>([]);

const queryParams = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  roleId: undefined,
  userName: undefined,
  phoneNumber: undefined
});

const tableRef = ref<ElTableInstance>();
const queryFormRef = ref<ElFormInstance>();

const show = () => {
  queryParams.roleId = props.roleId;
  getList();
  visible.value = true;
};

/**
 * 选择行
 */
const clickRow = (row: any) => {
  // ele的bug
  tableRef.value?.toggleRowSelection(row, false);
};
/** 多选框选中数据 */
const handleSelectionChange = (selection: UserVO[]) => {
  userIds.value = selection.map((item: UserVO) => item.userId);
};

/** 查询数据 */
const getList = async () => {
  const res = await unallocatedUserList(queryParams);
  userList.value = res.data?.rows;
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
  getList();
};

const emit = defineEmits(['ok']);
/**选择授权用户操作 */
const handleSelectUser = async () => {
  const roleId = queryParams.roleId;
  const ids = userIds.value.join(',');
  if (ids == '') {
    modal.msgError('请选择要分配的用户');
    return;
  }
  await authUserSelectAll({ roleId, userIds: ids });
  modal.msgSuccess('分配成功');
  emit('ok');
  visible.value = false;
};
// 暴露
defineExpose({
  show
});
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/selector-dialog' as selectorDialog;

@include selectorDialog.shell-gap('.select-user-shell');
@include selectorDialog.dialog-body-padding('select-user-dialog');

.select-user-card :deep(.el-card__body) {
  padding: 14px 16px !important;
}
</style>
