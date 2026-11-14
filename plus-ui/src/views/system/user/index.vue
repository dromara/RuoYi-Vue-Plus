<template>
  <div class="p-2 system-user-page">
    <el-row :gutter="20" class="content-grid">
      <!-- 部门树 -->
      <tree-panel
        ref="treePanelRef"
        v-model:collapsed="treeCollapsed"
        title="部门结构"
        placeholder="请输入部门名称"
        :data="deptOptions"
        :expanded-span="5"
        @node-click="handleNodeClick"
      />
      <el-col
        :lg="treeCollapsed ? 23 : 19"
        :xs="24"
        class="tree-content-col content-main"
        :class="{ 'is-tree-collapsed': treeCollapsed }"
      >
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
              <el-form-item label="用户名称" prop="userName">
                <el-input
                  v-model="queryParams.userName"
                  placeholder="请输入用户名称"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
              <el-form-item label="用户昵称" prop="nickName">
                <el-input
                  v-model="queryParams.nickName"
                  placeholder="请输入用户昵称"
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

              <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="用户状态" clearable>
                  <el-option
                    v-for="dict in sys_normal_disable"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="创建时间" style="width: 308px">
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
                <span class="panel-kicker">User Dataset</span>
                <h3>用户列表</h3>
                <p>共 {{ total }} 条记录，支持部门筛选、状态切换、导入导出和角色分配。</p>
              </div>
              <div class="toolbar-actions">
                <el-button v-has-permi="['system:user:add']" type="primary" plain icon="Plus" @click="handleAdd()">
                  新增
                </el-button>
                <el-button
                  v-has-permi="['system:user:edit']"
                  type="success"
                  plain
                  :disabled="single"
                  icon="Edit"
                  @click="handleUpdate()"
                >
                  修改
                </el-button>
                <el-button
                  v-has-permi="['system:user:remove']"
                  type="danger"
                  plain
                  :disabled="multiple"
                  icon="Delete"
                  @click="handleDelete()"
                >
                  删除
                </el-button>
                <el-button
                  v-hasPermi="['system:user:edit']"
                  type="warning"
                  plain
                  icon="Unlock"
                  :disabled="single"
                  @click="handleUnlock()"
                >
                  解锁
                </el-button>
                <el-dropdown class="mt-[1px]">
                  <el-button plain type="info">
                    更多
                    <el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item icon="Download" @click="importTemplate">下载模板</el-dropdown-item>
                      <!-- 注意 由于el-dropdown-item标签是延迟加载的 所以v-has-permi自定义标签不生效 需要使用v-if调用方法执行 -->
                      <el-dropdown-item v-if="checkPermi(['system:user:import'])" icon="Top" @click="handleImport">
                        导入数据
                      </el-dropdown-item>
                      <el-dropdown-item v-if="checkPermi(['system:user:export'])" icon="Download" @click="handleExport">
                        导出数据
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <right-toolbar
                  v-model:show-search="showSearch"
                  :columns="columns"
                  :search="false"
                  storage-key="sys_user_column_visible"
                  @query-table="getList"
                ></right-toolbar>
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
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column v-if="columns[0].visible" key="userId" label="用户编号" align="center" prop="userId" />
            <el-table-column
              v-if="columns[1].visible"
              key="userName"
              label="用户名称"
              align="center"
              :show-overflow-tooltip="true"
            >
              <template #default="scope">
                <el-link type="primary" underline="never" @click="handleViewDetail(scope.row)">
                  {{ scope.row.userName }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column
              v-if="columns[2].visible"
              key="nickName"
              label="用户昵称"
              align="center"
              prop="nickName"
              :show-overflow-tooltip="true"
            />
            <el-table-column
              v-if="columns[3].visible"
              key="deptName"
              label="部门"
              align="center"
              prop="deptName"
              :show-overflow-tooltip="true"
            />
            <el-table-column
              v-if="columns[4].visible"
              key="phoneNumber"
              label="手机号码"
              align="center"
              prop="phoneNumber"
              width="120"
            />
            <el-table-column v-if="columns[5].visible" key="status" label="状态" align="center">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleStatusChange(scope.row)"
                ></el-switch>
              </template>
            </el-table-column>

            <el-table-column v-if="columns[6].visible" label="创建时间" align="center" prop="createTime" width="160">
              <template #default="scope">
                <span>{{ scope.row.createTime }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" fixed="right" width="180" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-tooltip v-if="scope.row.userId !== '1761100000000000001'" content="修改" placement="top">
                  <el-button
                    v-hasPermi="['system:user:edit']"
                    link
                    type="primary"
                    icon="Edit"
                    @click="handleUpdate(scope.row)"
                  ></el-button>
                </el-tooltip>
                <el-tooltip v-if="scope.row.userId !== '1761100000000000001'" content="删除" placement="top">
                  <el-button
                    v-hasPermi="['system:user:remove']"
                    link
                    type="primary"
                    icon="Delete"
                    @click="handleDelete(scope.row)"
                  ></el-button>
                </el-tooltip>

                <el-tooltip v-if="scope.row.userId !== '1761100000000000001'" content="重置密码" placement="top">
                  <el-button
                    v-hasPermi="['system:user:resetPwd']"
                    link
                    type="primary"
                    icon="Key"
                    @click="handleResetPwd(scope.row)"
                  ></el-button>
                </el-tooltip>

                <el-tooltip v-if="scope.row.userId !== '1761100000000000001'" content="分配角色" placement="top">
                  <el-button
                    v-hasPermi="['system:user:edit']"
                    link
                    type="primary"
                    icon="CircleCheck"
                    @click="handleAuthRole(scope.row)"
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
      </el-col>
    </el-row>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog
      ref="formDialogRef"
      v-model="dialog.visible"
      :title="dialog.title"
      width="600px"
      append-to-body
      @close="closeDialog"
    >
      <el-form ref="userFormRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.userId == null || form.userId != useUserStore().userId">
            <el-form-item label="归属部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="enabledDeptOptions"
                :props="{ value: 'id', label: 'label', children: 'children' } as any"
                value-key="id"
                placeholder="请选择归属部门"
                check-strictly
                @change="handleDeptChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phoneNumber">
              <el-input v-model="form.phoneNumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password">
              <el-input
                v-model="form.password"
                placeholder="请输入用户密码"
                type="password"
                maxlength="20"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.gender" placeholder="请选择">
                <el-option
                  v-for="dict in sys_user_gender"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12" v-if="form.userId == null || form.userId != useUserStore().userId">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择">
                <el-option
                  v-for="item in postOptions"
                  :key="item.postId"
                  :label="item.postName"
                  :value="item.postId"
                  :disabled="item.status == '1'"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.userId == null || form.userId != useUserStore().userId">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" filterable multiple placeholder="请选择">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status == '1'"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel()">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog v-model="upload.open" :title="upload.title" width="400px" append-to-body>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload">
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或
          <em>点击上传</em>
        </div>
        <template #tip>
          <div class="text-center el-upload__tip">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />
              是否更新已经存在的用户数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link
              type="primary"
              underline="never"
              style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate"
            >
              下载模板
            </el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户详情抽屉 -->
    <user-view-drawer ref="userViewRef" />
  </div>
</template>

<script setup name="User" lang="ts">
import { to } from 'await-to-js';
import { useRouter } from 'vue-router';
import { getConfigKey } from '@/api/system/config';
import { DeptTreeVO, DeptVO } from '@/api/system/dept/types';
import { optionselect } from '@/api/system/post';
import { PostVO } from '@/api/system/post/types';
import { RoleVO } from '@/api/system/role/types';
import api from '@/api/system/user';
import { UserForm, UserQuery, UserVO } from '@/api/system/user/types';
import TreePanel from '@/components/TreePanel/index.vue';
import { useLoading } from '@/hooks/async/useLoading';
import { useDialogState } from '@/hooks/dialog/useDialogState';
import { useDateRangeQuery } from '@/hooks/form/useDateRangeQuery';
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTableSelection } from '@/hooks/table/useTableSelection';
import { useTreeCollapsed } from '@/hooks/tree/useTreeCollapsed';
import modal from '@/plugins/modal';
import { useUserStore } from '@/store/modules/user';
import { useDict } from '@/utils/dict';
import { checkPermi } from '@/utils/permission';
import { globalHeaders } from '@/utils/request';
import { download as requestDownload } from '@/utils/request';
import UserViewDrawer from './view.vue';

const router = useRouter();
const { sys_normal_disable, sys_user_gender } = toRefs<any>(useDict('sys_normal_disable', 'sys_user_gender'));
const userList = ref<UserVO[]>();
const { loading, withLoading } = useLoading(true);
const { showSearch } = useSearchToggle();
const total = ref(0);
const { dateRange, applyDateRange, resetDateRange } = useDateRangeQuery();
const { treeCollapsed } = useTreeCollapsed();
const deptOptions = ref<DeptTreeVO[]>([]);
const enabledDeptOptions = ref<DeptTreeVO[]>([]);
const initPassword = ref<string>('');
const postOptions = ref<PostVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
/*** 用户导入参数 */
const upload = reactive<ImportOption>({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: '',
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: globalHeaders(),
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + '/system/user/importData'
});
// 列显隐信息
const columns = ref<FieldOption[]>([
  { key: 0, label: `用户编号`, visible: false, children: [] },
  { key: 1, label: `用户名称`, visible: true, children: [] },
  { key: 2, label: `用户昵称`, visible: true, children: [] },
  { key: 3, label: `部门`, visible: true, children: [] },
  { key: 4, label: `手机号码`, visible: true, children: [] },
  { key: 5, label: `状态`, visible: true, children: [] },
  { key: 6, label: `创建时间`, visible: true, children: [] }
]);

const treePanelRef = ref<InstanceType<typeof TreePanel>>();
const queryFormRef = ref<ElFormInstance>();
const userFormRef = ref<ElFormInstance>();
const uploadRef = ref<ElUploadInstance>();
const formDialogRef = ref<ElDialogInstance>();
const userViewRef = ref<InstanceType<typeof UserViewDrawer>>();

const initFormData: UserForm = {
  userId: undefined,
  deptId: undefined,
  userName: '',
  nickName: undefined,
  password: '',
  phoneNumber: undefined,
  email: undefined,
  gender: undefined,
  status: '0',
  remark: '',
  postIds: [],
  roleIds: []
};

const initData: PageData<UserForm, UserQuery> = {
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: '',
    phoneNumber: '',
    status: '',
    deptId: '',
    roleId: ''
  },
  rules: {
    userName: [
      { required: true, message: '用户名称不能为空', trigger: 'blur' },
      {
        min: 2,
        max: 20,
        message: '用户名称长度必须介于 2 和 20 之间',
        trigger: 'blur'
      }
    ],
    nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
    password: [
      { required: true, message: '用户密码不能为空', trigger: 'blur' },
      {
        min: 5,
        max: 20,
        message: '用户密码长度必须介于 5 和 20 之间',
        trigger: 'blur'
      },
      {
        pattern: /^[^<>"'|\\]+$/,
        message: '不能包含非法字符：< > " \' \\ |',
        trigger: 'blur'
      }
    ],
    email: [
      {
        type: 'email',
        message: '请输入正确的邮箱地址',
        trigger: ['blur', 'change']
      }
    ],
    phoneNumber: [
      {
        pattern: /^1[3456789][0-9]\d{8}$/,
        message: '请输入正确的手机号码',
        trigger: 'blur'
      }
    ],
    roleIds: [{ required: true, message: '用户角色不能为空', trigger: 'blur' }]
  }
};
const data = reactive<PageData<UserForm, UserQuery>>(initData);

const { queryParams, form, rules } = toRefs<PageData<UserForm, UserQuery>>(data);
const { ids, single, multiple, handleSelectionChange } = useTableSelection<UserVO>(item => item.userId);
const { dialog, openDialog: openUserDialog, closeDialog: closeUserDialog, setTitle: setDialogTitle } = useDialogState();

/** 查询用户列表 */
const getList = async () => {
  await withLoading(async () => {
    const res = await api.listUser(applyDateRange(queryParams.value));
    userList.value = res.data?.rows;
    total.value = res.data?.total;
  });
};

/** 查询部门下拉树结构 */
const getDeptTree = async () => {
  const res = await api.deptTreeSelect();
  deptOptions.value = res.data;
  enabledDeptOptions.value = filterDisabledDept(res.data);
};

/** 过滤禁用的部门，并返回独立的新树，避免污染左侧完整部门树 */
const filterDisabledDept = (deptList: DeptTreeVO[]): DeptTreeVO[] => {
  return deptList.reduce<DeptTreeVO[]>((result, dept) => {
    if (dept.disabled) {
      return result;
    }
    result.push({
      ...dept,
      children: dept.children?.length ? filterDisabledDept(dept.children) : []
    });
    return result;
  }, []);
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
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  resetExtras: () => {
    resetDateRange();
    queryParams.value.deptId = undefined;
    treePanelRef.value?.setCurrentKey(undefined);
  },
  afterReset: () => {
    handleQuery();
  }
});

/** 删除按钮操作 */
const handleDelete = async (row?: Partial<UserVO>) => {
  const userIds = row?.userId || ids.value;
  const [err] = await to(modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？') as any);
  if (!err) {
    await api.delUser(userIds);
    await getList();
    modal.msgSuccess('删除成功');
  }
};

/** 解锁按钮操作 */
const handleUnlock = async () => {
  const userId = ids.value[0];
  const [err] = await to(modal.confirm('是否确认解锁用户编号为"' + userId + '"的数据项?') as any);
  if (!err) {
    await api.unlockUser(userId);
    modal.msgSuccess('用户编号"' + userId + '"解锁成功');
  }
};

/** 用户状态修改  */
const handleStatusChange = async (row: Partial<UserVO>) => {
  const text = row.status === '0' ? '启用' : '停用';
  try {
    await modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗?');
    await api.changeUserStatus(row.userId, row.status);
    modal.msgSuccess(text + '成功');
  } catch (err) {
    row.status = row.status === '0' ? '1' : '0';
  }
};
/** 跳转角色分配 */
const handleAuthRole = (row: Partial<UserVO>) => {
  const userId = row.userId;
  router.push('/system/user-auth/role/' + userId);
};

/** 重置密码按钮操作 */
const handleResetPwd = async (row: Partial<UserVO>) => {
  const [err, res] = await to(
    ElMessageBox.prompt('请输入"' + row.userName + '"的新密码', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      inputPattern: /^.{5,20}$/,
      inputErrorMessage: '用户密码长度必须介于 5 和 20 之间',
      inputValidator: value => {
        if (/<|>|"|'|\||\\/.test(value)) {
          return '不能包含非法字符：< > " \' \\ |';
        }
      }
    })
  );
  if (!err && res) {
    await api.resetUserPwd(row.userId, res.value);
    modal.msgSuccess('修改成功，新密码是：' + res.value);
  }
};

/** 详情按钮操作 */
const handleViewDetail = (row: Partial<UserVO>) => {
  userViewRef.value?.openDrawer(row.userId);
};

/** 导入按钮操作 */
const handleImport = () => {
  upload.title = '用户导入';
  upload.open = true;
};
/** 导出按钮操作 */
const handleExport = () => {
  requestDownload(
    'system/user/export',
    {
      ...queryParams.value
    },
    `user_${new Date().getTime()}.xlsx`
  );
};
/** 下载模板操作 */
const importTemplate = () => {
  requestDownload('system/user/importTemplate', {}, `user_template_${new Date().getTime()}.xlsx`);
};

/**文件上传中处理 */
const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

const formatImportResultMessage = (message: unknown) => {
  return String(message ?? '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, '');
};

/** 文件上传成功处理 */
const handleFileSuccess = (response: any, file: UploadFile) => {
  upload.open = false;
  upload.isUploading = false;
  uploadRef.value?.handleRemove(file);
  ElMessageBox.alert(formatImportResultMessage(response.msg), '导入结果', {
    customClass: 'import-result-box'
  });
  getList();
};

/** 提交上传文件 */
function submitFileForm() {
  uploadRef.value?.submit();
}

/** 重置操作表单 */
const reset = () => {
  form.value = { ...initFormData };
  userFormRef.value?.resetFields();
};
/** 取消按钮 */
const cancel = () => {
  closeUserDialog();
  reset();
};

/** 新增按钮操作 */
const handleAdd = async () => {
  reset();
  const { data } = await api.getUser();
  setDialogTitle('新增用户');
  openUserDialog();
  postOptions.value = data.posts;
  roleOptions.value = data.roles;
  form.value.password = initPassword.value.toString();
};

/** 修改按钮操作 */
const handleUpdate = async (row?: Partial<UserForm>) => {
  reset();
  const userId = row?.userId || ids.value[0];
  const { data } = await api.getUser(userId);
  setDialogTitle('修改用户');
  openUserDialog();
  Object.assign(form.value, data.user);
  postOptions.value = data.posts;
  roleOptions.value = Array.from(
    new Map([...data.roles, ...data.user.roles].map(role => [role.roleId, role])).values()
  );
  form.value.postIds = data.postIds;
  form.value.roleIds = data.roleIds;
  form.value.password = '';
};

/** 提交按钮 */
const submitForm = () => {
  userFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      if (form.value.userId) {
        // 自己编辑自己的情况下 不允许编辑角色部门岗位
        if (form.value.userId == useUserStore().userId) {
          form.value.roleIds = null;
          form.value.deptId = null;
          form.value.postIds = null;
        }
        await api.updateUser(form.value);
      } else {
        await api.addUser(form.value);
      }
      modal.msgSuccess('操作成功');
      closeUserDialog();
      await getList();
    }
  });
};

/**
 * 关闭用户弹窗
 */
const closeDialog = () => {
  closeUserDialog();
  resetForm();
};

/**
 * 重置表单
 */
const resetForm = () => {
  userFormRef.value?.resetFields();
  userFormRef.value?.clearValidate();

  form.value.id = undefined;
  form.value.status = '1';
};
onMounted(() => {
  getDeptTree(); // 初始化部门数据
  getList(); // 初始化列表数据
  getConfigKey('sys.user.initPassword').then(response => {
    initPassword.value = response.data;
  });
});

async function handleDeptChange(value: number | string) {
  const response = await optionselect(value);
  postOptions.value = response.data;
  form.value.postIds = [];
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/components/page-shell' as pageShell;

@include pageShell.tree-table-crud-page;

:global(.import-result-box .el-message-box__message) {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 20px 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
