<template>
  <div class="p-2 app-container auth-role-page">
    <el-card shadow="hover" class="search-panel auth-role-info">
      <template #header>
        <div class="panel-heading">
          <div><h3>基本信息</h3></div>
        </div>
      </template>
      <el-form :model="form" :inline="true" class="query-form auth-role-form">
        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="form.nickName" disabled />
        </el-form-item>
        <el-form-item label="登录账号" prop="userName">
          <el-input v-model="form.userName" disabled />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>角色信息</h3>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="submitForm()">提交</el-button>
            <el-button @click="close()">返回</el-button>
          </div>
        </div>
      </template>
      <el-table
        ref="tableRef"
        v-loading="loading"
        border
        class="data-table"
        :row-key="getRowKey"
        :data="roles.slice((pageNum - 1) * pageSize, pageNum * pageSize)"
        @row-click="clickRow"
        @selection-change="handleSelectionChange"
      >
        <el-table-column label="序号" width="55" type="index" align="center">
          <template #default="scope">
            <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column
          type="selection"
          :reserve-selection="true"
          :selectable="checkSelectable"
          width="55"
        ></el-table-column>
        <el-table-column label="角色编号" align="center" prop="roleId" />
        <el-table-column label="角色名称" align="center" prop="roleName" />
        <el-table-column label="权限字符" align="center" prop="roleKey" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="pageNum" v-model:limit="pageSize" :total="total" />
    </el-card>
  </div>
</template>

<script setup name="AuthRole" lang="ts">
import { RouteLocationNormalized } from 'vue-router';
import { RoleVO } from '@/api/system/role/types';
import { getAuthRole, updateAuthRole } from '@/api/system/user';
import { UserForm } from '@/api/system/user/types';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';
import { parseTime } from '@/utils/ruoyi';

const route = useRoute();

const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const roleIds = ref<Array<string | number>>([]);
const roles = ref<RoleVO[]>([]);
const form = ref<Partial<UserForm>>({
  nickName: undefined,
  userName: '',
  userId: undefined
});

const tableRef = ref<ElTableInstance>();

/** 单击选中行数据 */
const clickRow = (row: RoleVO) => {
  if (checkSelectable(row)) {
    row.flag = !row.flag;
    tableRef.value?.toggleRowSelection(row, row.flag);
  }
};
/** 多选框选中数据 */
const handleSelectionChange = (selection: RoleVO[]) => {
  roleIds.value = selection.map(item => item.roleId);
};
/** 保存选中的数据编号 */
const getRowKey = (row: RoleVO): string => {
  return String(row.roleId);
};
/** 检查角色状态 */
const checkSelectable = (row: RoleVO): boolean => {
  return row.status === '0';
};
/** 关闭按钮 */
const close = () => {
  const obj: RouteLocationNormalized = {
    fullPath: '',
    hash: '',
    matched: [],
    meta: undefined,
    name: undefined,
    params: undefined,
    query: undefined,
    redirectedFrom: undefined,
    path: '/system/user'
  };
  tab.closeOpenPage(obj as any);
};
/** 提交按钮 */
const submitForm = async () => {
  const userId = form.value.userId;
  const rIds = roleIds.value.join(',');
  await updateAuthRole({ userId: userId as string, roleIds: rIds });
  modal.msgSuccess('授权成功');
  close();
};

const getList = async () => {
  const userId = route.params && route.params.userId;
  if (userId) {
    loading.value = true;
    const res = await getAuthRole(userId as string);
    Object.assign(form.value, res.data.user);
    Object.assign(roles.value, res.data.roles);
    total.value = roles.value.length;
    await nextTick(() => {
      roles.value.forEach(row => {
        if (row?.flag) {
          tableRef.value?.toggleRowSelection(row, true);
        }
      });
    });
    loading.value = false;
  }
};
onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.auth-role-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.auth-role-form :deep(.el-input) {
  width: 220px;
}

.auth-role-info :deep(.el-card__body) {
  padding-top: 14px !important;
}
</style>
