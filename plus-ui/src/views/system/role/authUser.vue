<template>
  <div class="p-2 app-container role-auth-user-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div><h3>筛选条件</h3></div>
          </div>
        </template>
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
    </div>
    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>已授权用户</h3>
          </div>
          <div class="toolbar-actions">
            <el-button v-hasPermi="['system:role:add']" type="primary" plain icon="Plus" @click="openSelectUser">
              添加用户
            </el-button>
            <el-button
              v-hasPermi="['system:role:remove']"
              type="danger"
              plain
              icon="CircleClose"
              :disabled="multiple"
              @click="cancelAuthUserAll"
            >
              批量取消授权
            </el-button>
            <el-button type="warning" plain icon="Close" @click="handleClose">关闭</el-button>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>
      <el-table
        v-loading="loading"
        border
        class="data-table"
        :data="userList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
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
            <span>{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="取消授权" placement="top">
              <el-button
                v-hasPermi="['system:role:remove']"
                link
                type="primary"
                icon="CircleClose"
                @click="cancelAuthUser(scope.row)"
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
      <select-user ref="selectRef" :role-id="queryParams.roleId" @ok="handleQuery" />
    </el-card>
  </div>
</template>

<script setup name="AuthUser" lang="ts">
import { RouteLocationNormalized } from 'vue-router';
import { allocatedUserList, authUserCancel, authUserCancelAll } from '@/api/system/role';
import { UserQuery } from '@/api/system/user/types';
import { UserVO } from '@/api/system/user/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';
import { useDict } from '@/utils/dict';
import SelectUser from './selectUser.vue';

const route = useRoute();
const { sys_normal_disable } = toRefs<any>(useDict('sys_normal_disable'));

const userList = ref<UserVO[]>([]);
const { loading, withLoading } = useLoading(true);
const { showSearch } = useSearchToggle();
const total = ref(0);

const queryFormRef = ref<ElFormInstance>();
const selectRef = ref<InstanceType<typeof SelectUser>>();

const queryParams = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  roleId: route.params.roleId as string,
  userName: undefined,
  phoneNumber: undefined
});
const { ids: userIds, multiple, handleSelectionChange } = useTableSelection<UserVO>(item => item.userId);

/** 查询授权用户列表 */
const getList = async () => {
  await withLoading(async () => {
    const res = await allocatedUserList(queryParams);
    userList.value = res.data?.rows;
    total.value = res.data?.total;
  });
};
// 返回按钮
const handleClose = () => {
  const obj: RouteLocationNormalized = {
    path: '/system/role',
    fullPath: '',
    hash: '',
    matched: [],
    meta: undefined,
    name: undefined,
    params: undefined,
    query: undefined,
    redirectedFrom: undefined
  };
  tab.closeOpenPage(obj as any);
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};
const { resetQuery } = useSearchReset({
  queryFormRef,
  afterReset: () => {
    handleQuery();
  }
});
/** 打开授权用户表弹窗 */
const openSelectUser = () => {
  selectRef.value?.show();
};
/** 取消授权按钮操作 */
const cancelAuthUser = async (row: Partial<UserVO>) => {
  await modal.confirm('确认要取消该用户"' + row.userName + '"角色吗？');
  await authUserCancel({ userId: row.userId, roleId: queryParams.roleId });
  await getList();
  modal.msgSuccess('取消授权成功');
};
/** 批量取消授权按钮操作 */
const cancelAuthUserAll = async () => {
  const roleId = queryParams.roleId;
  const uIds = userIds.value.join(',');
  await modal.confirm('是否取消选中用户授权数据项?');
  await authUserCancelAll({ roleId: roleId, userIds: uIds });
  await getList();
  modal.msgSuccess('取消授权成功');
};

onMounted(() => {
  getList();
});
</script>
