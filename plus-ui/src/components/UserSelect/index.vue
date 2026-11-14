<template>
  <div>
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      width="80%"
      append-to-body
      class="user-select-dialog"
    >
      <div class="p-2 user-select-shell">
        <el-row :gutter="12" class="selector-layout">
          <!-- 部门树 -->
          <el-col
            :lg="treeCollapsed ? 1 : 5"
            :xs="24"
            class="tree-panel-col"
            :class="{ 'is-collapsed': treeCollapsed }"
          >
            <el-card
              shadow="hover"
              class="side-panel tree-panel-shell selector-card selector-side-card"
              :class="{ 'is-collapsed': treeCollapsed }"
            >
              <template #header>
                <div
                  class="panel-heading search-panel-toggle tree-panel-header"
                  :class="{ 'is-collapsed': treeCollapsed }"
                  @click.stop="treeCollapsed = !treeCollapsed"
                >
                  <div v-show="!treeCollapsed" class="table-heading">
                    <h3>部门结构</h3>
                  </div>
                </div>
              </template>
              <template v-if="!treeCollapsed">
                <el-input
                  v-model="deptName"
                  class="selector-dept-input"
                  placeholder="请输入部门名称"
                  prefix-icon="Search"
                  clearable
                />
                <el-tree
                  ref="deptTreeRef"
                  class="selector-tree"
                  node-key="id"
                  :data="deptOptions"
                  :props="{ label: 'label', children: 'children' } as any"
                  :expand-on-click-node="false"
                  :filter-node-method="filterNode"
                  highlight-current
                  default-expand-all
                  @node-click="handleNodeClick"
                />
              </template>
            </el-card>
          </el-col>
          <el-col
            :lg="treeCollapsed ? 23 : 19"
            :xs="24"
            class="tree-content-col"
            :class="{ 'is-tree-collapsed': treeCollapsed }"
          >
            <div class="p-2user-select-main">
              <transition
                :enter-active-class="animateConfig.searchAnimate.enter"
                :leave-active-class="animateConfig.searchAnimate.leave"
              >
                <div v-show="showSearch">
                  <el-card shadow="hover" class="search-panel selector-card">
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
                        <el-button icon="Refresh" @click="() => resetQuery()">重置</el-button>
                      </el-form-item>
                    </el-form>
                  </el-card>
                </div>
              </transition>

              <el-card shadow="hover" class="table-panel selector-card">
                <template #header>
                  <div class="toolbar-shell selector-header">
                    <div class="table-heading">
                      <h3>用户列表</h3>
                    </div>
                    <div v-if="prop.multiple && selectUserList.length" class="selector-tags">
                      <el-tag v-for="user in selectUserList" :key="user.userId" closable @close="handleCloseTag(user)">
                        {{ user.nickName }}
                      </el-tag>
                    </div>
                  </div>
                </template>

                <vxe-table
                  ref="tableRef"
                  class="selector-table"
                  height="400px"
                  border
                  show-overflow
                  :data="userList"
                  :loading="loading"
                  :row-config="{ keyField: 'userId', isHover: true }"
                  :checkbox-config="{
                    reserve: true,
                    trigger: 'row',
                    highlight: true,
                    showHeader: prop.multiple
                  }"
                  @checkbox-all="handleCheckboxAll"
                  @checkbox-change="handleCheckboxChange"
                >
                  <vxe-column type="checkbox" width="50" align="center" />
                  <vxe-column key="userId" title="用户编号" align="center" field="userId" />
                  <vxe-column key="userName" title="用户名称" align="center" field="userName" />
                  <vxe-column key="nickName" title="用户昵称" align="center" field="nickName" />
                  <vxe-column key="deptName" title="部门" align="center" field="deptName" />
                  <vxe-column key="phoneNumber" title="手机号码" align="center" field="phoneNumber" width="120" />
                  <vxe-column key="status" title="状态" align="center">
                    <template #default="scope">
                      <dict-tag :options="sys_normal_disable" :value="scope.row.status"></dict-tag>
                    </template>
                  </vxe-column>

                  <vxe-column title="创建时间" align="center" width="160">
                    <template #default="scope">
                      <span>{{ scope.row.createTime }}</span>
                    </template>
                  </vxe-column>
                </vxe-table>

                <pagination
                  v-show="total > 0"
                  v-model:page="queryParams.pageNum"
                  v-model:limit="queryParams.pageSize"
                  :total="total"
                  @pagination="pageList"
                />
              </el-card>
            </div>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { VxeTableInstance } from 'vxe-table';
import animateConfig from '@/animate';
import { DeptTreeVO, DeptVO } from '@/api/system/dept/types';
import api from '@/api/system/user';
import { UserQuery, UserVO } from '@/api/system/user/types';
import { useDialogState } from '@/hooks/dialog/useDialogState';
import { useDateRangeQuery } from '@/hooks/form/useDateRangeQuery';
import { useDict } from '@/utils/dict';

interface PropType {
  modelValue?: UserVO[] | UserVO | undefined;
  multiple?: boolean;
  data?: string | number | (string | number)[] | undefined;
  userIds?: string | number | (string | number)[] | undefined;
}
const prop = withDefaults(defineProps<PropType>(), {
  multiple: true,
  modelValue: undefined,
  data: undefined,
  userIds: undefined
});
const emit = defineEmits(['update:modelValue', 'confirmCallBack']);

const { sys_normal_disable } = toRefs<any>(useDict('sys_normal_disable'));

const userList = ref<UserVO[]>();
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const { dateRange, applyDateRange, resetDateRange } = useDateRangeQuery();
const deptName = ref('');
const treeCollapsed = ref(false);
const deptOptions = ref<DeptTreeVO[]>([]);
const selectUserList = ref<UserVO[]>([]);

const deptTreeRef = ref<ElTreeInstance>();
const queryFormRef = ref<ElFormInstance>();
const tableRef = ref<VxeTableInstance<UserVO>>();

const { dialog, openDialog, closeDialog } = useDialogState('用户选择');

const queryParams = ref<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  phoneNumber: '',
  status: '',
  deptId: '',
  roleId: '',
  userIds: ''
});

const defaultSelectUserIds = computed(() => computedIds(prop.data));

/** 根据名称筛选部门树 */
watchEffect(
  () => {
    deptTreeRef.value?.filter(deptName.value);
  },
  {
    flush: 'post' // watchEffect会在DOM挂载或者更新之前就会触发，此属性控制在DOM元素更新后运行
  }
);

const confirm = () => {
  emit('update:modelValue', selectUserList.value);
  emit('confirmCallBack', selectUserList.value);
  closeDialog();
};

const computedIds = data => {
  if (data === '' || data === null || data === undefined) {
    return [];
  }
  if (data instanceof Array) {
    return data.map(item => String(item));
  } else if (typeof data === 'string') {
    return data.split(',');
  } else if (typeof data === 'number') {
    return [String(data)];
  } else {
    console.warn('<UserSelect> The data type of data should be array or string or number, but I received other');
    return [];
  }
};

/** 通过条件过滤节点  */
const filterNode = (value: string, data: any) => {
  if (!value) return true;
  return data.label.indexOf(value) !== -1;
};

/** 查询部门下拉树结构 */
const getTreeSelect = async () => {
  const res = await api.deptTreeSelect();
  deptOptions.value = res.data;
};

/** 查询用户列表 */
const getList = async () => {
  loading.value = true;
  queryParams.value.userIds = prop.userIds;
  const res = await api.listUser(applyDateRange(queryParams.value));
  loading.value = false;
  userList.value = res.data?.rows;
  total.value = res.data?.total;
};

const pageList = async () => {
  await getList();
  const users = userList.value.filter(item => {
    return selectUserList.value.some(user => user.userId === item.userId);
  });
  await tableRef.value.setCheckboxRow(users, true);
};

/** 节点单击事件 */
const handleNodeClick = (data: DeptVO) => {
  queryParams.value.deptId = data.id;
  handleQuery();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
/** 重置按钮操作 */
const resetQuery = (refresh = true) => {
  resetDateRange();
  queryFormRef.value?.resetFields();
  queryParams.value.pageNum = 1;
  queryParams.value.deptId = undefined;
  deptTreeRef.value?.setCurrentKey(undefined);
  refresh && handleQuery();
};

const handleCheckboxChange = checked => {
  if (!prop.multiple && checked.checked) {
    tableRef.value.setCheckboxRow(selectUserList.value, false);
    selectUserList.value = [];
  }
  const row = checked.row;
  if (checked.checked) {
    selectUserList.value.push(row);
  } else {
    selectUserList.value = selectUserList.value.filter(item => {
      return item.userId !== row.userId;
    });
  }
};
const handleCheckboxAll = checked => {
  const rows = userList.value;
  if (checked.checked) {
    rows.forEach(row => {
      if (!selectUserList.value.some(item => item.userId === row.userId)) {
        selectUserList.value.push(row);
      }
    });
  } else {
    selectUserList.value = selectUserList.value.filter(item => {
      return !rows.some(row => row.userId === item.userId);
    });
  }
};

const handleCloseTag = (user: UserVO) => {
  const userId = user.userId;
  // 使用split删除用户
  const index = selectUserList.value.findIndex(item => item.userId === userId);
  const rows = selectUserList.value[index];
  tableRef.value?.setCheckboxRow(rows, false);
  selectUserList.value.splice(index, 1);
};

const initSelectUser = async () => {
  if (defaultSelectUserIds.value.length > 0) {
    const { data } = await api.optionSelect(defaultSelectUserIds.value);
    selectUserList.value = data;
    const users = userList.value.filter(item => {
      return defaultSelectUserIds.value.includes(String(item.userId));
    });
    await nextTick(() => {
      tableRef.value.setCheckboxRow(users, true);
    });
  }
};
const close = () => {
  closeDialog();
};

watch(
  () => dialog.visible,
  async (newValue: boolean) => {
    if (newValue) {
      await getTreeSelect(); // 初始化部门数据
      await getList(); // 初始化列表数据
      await initSelectUser();
    } else {
      tableRef.value.clearCheckboxReserve();
      tableRef.value.clearCheckboxRow();
      resetQuery(false);
      selectUserList.value = [];
    }
  }
);

defineExpose({
  open: openDialog,
  close: closeDialog
});
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/page-shell' as pageShell;
@use '@/assets/styles/components/selector-dialog' as selectorDialog;

@include pageShell.collapsible-tree-layout(992px);
@include selectorDialog.shell-gap('.user-select-shell', '.user-select-main');
@include selectorDialog.card-shell;
@include selectorDialog.selector-header-tags(992px);
@include selectorDialog.dialog-body-padding('user-select-dialog');
@include selectorDialog.selector-table;

.selector-layout {
  align-items: stretch;
}

.selector-side-card {
  min-height: 100%;
}

.selector-dept-input {
  margin-bottom: 12px;
}

.selector-tree {
  min-height: 400px;
}

@media (max-width: 992px) {
  .selector-tree {
    min-height: 220px;
  }
}
</style>
