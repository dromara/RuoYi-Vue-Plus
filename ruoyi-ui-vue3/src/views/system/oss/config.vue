<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="配置key" prop="configKey">
        <el-input
          v-model="queryParams.configKey"
          placeholder="配置key"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="桶名称" prop="bucketName">
        <el-input
          v-model="queryParams.bucketName"
          placeholder="请输入桶名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否默认" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option key="0" label="是" value="0"/>
          <el-option key="1" label="否" value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:oss:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:oss:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:oss:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ossConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主建" align="center" prop="ossConfigId" v-if="columns[0].visible"/>
      <el-table-column label="配置key" align="center" prop="configKey" v-if="columns[1].visible" />
      <el-table-column label="访问站点" align="center" prop="endpoint" v-if="columns[2].visible" width="200" />
      <el-table-column label="自定义域名" align="center" prop="domain" v-if="columns[3].visible" width="200" />
      <el-table-column label="桶名称" align="center" prop="bucketName" v-if="columns[4].visible" />
      <el-table-column label="前缀" align="center" prop="prefix" v-if="columns[5].visible" />
      <el-table-column label="域" align="center" prop="region" v-if="columns[6].visible" />
      <el-table-column label="桶权限类型" align="center" prop="accessPolicy" v-if="columns[7].visible" >
        <template #default="scope">
          <el-tag type="warning" v-if="scope.row.accessPolicy === '0'">private</el-tag>
          <el-tag type="success" v-if="scope.row.accessPolicy === '1'">public</el-tag>
          <el-tag type="info" v-if="scope.row.accessPolicy === '2'">custom</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否默认" align="center" prop="status" v-if="columns[8].visible">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:oss:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:oss:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改对象存储配置对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="ossConfigRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="配置key" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入配置key" />
        </el-form-item>
        <el-form-item label="访问站点" prop="endpoint">
          <el-input v-model="form.endpoint" placeholder="请输入访问站点" />
        </el-form-item>
        <el-form-item label="自定义域名" prop="domain">
          <el-input v-model="form.domain" placeholder="请输入自定义域名" />
        </el-form-item>
        <el-form-item label="accessKey" prop="accessKey">
          <el-input v-model="form.accessKey" placeholder="请输入accessKey" />
        </el-form-item>
        <el-form-item label="secretKey" prop="secretKey">
          <el-input v-model="form.secretKey" placeholder="请输入秘钥" show-password />
        </el-form-item>
        <el-form-item label="桶名称" prop="bucketName">
          <el-input v-model="form.bucketName" placeholder="请输入桶名称" />
        </el-form-item>
        <el-form-item label="前缀" prop="prefix">
          <el-input v-model="form.prefix" placeholder="请输入前缀" />
        </el-form-item>
        <el-form-item label="是否HTTPS">
          <el-radio-group v-model="form.isHttps">
            <el-radio
              v-for="dict in sys_yes_no"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="桶权限类型">
          <el-radio-group v-model="form.accessPolicy">
            <el-radio label="0">private</el-radio>
            <el-radio label="1">public</el-radio>
            <el-radio label="2">custom</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="域" prop="region">
          <el-input v-model="form.region" placeholder="请输入域" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="OssConfig">

import {
  listOssConfig,
  getOssConfig,
  delOssConfig,
  addOssConfig,
  updateOssConfig,
  changeOssConfigStatus
} from "@/api/system/ossConfig";

const { proxy } = getCurrentInstance();
const { sys_yes_no } = proxy.useDict("sys_yes_no");

const ossConfigList = ref([]);
const open = ref(false);
const buttonLoading = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 列显隐信息
const columns = ref([
  { key: 0, label: `主建`, visible: true },
  { key: 1, label: `配置key`, visible: false },
  { key: 2, label: `访问站点`, visible: true },
  { key: 3, label: `自定义域名`, visible: true },
  { key: 4, label: `桶名称`, visible: true },
  { key: 5, label: `前缀`, visible: true },
  { key: 6, label: `域`, visible: true },
  { key: 7, label: `桶权限类型`, visible: true },
  { key: 8, label: `状态`, visible: true }
]);

const data = reactive({
  form: {},
  // 查询参数
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    configKey: undefined,
    bucketName: undefined,
    status: undefined,
  },
  rules: {
    configKey: [
      { required: true, message: "configKey不能为空", trigger: "blur" },
    ],
    accessKey: [
      { required: true, message: "accessKey不能为空", trigger: "blur" },
      {
        min: 2,
        max: 200,
        message: "accessKey长度必须介于 2 和 100 之间",
        trigger: "blur",
      },
    ],
    secretKey: [
      { required: true, message: "secretKey不能为空", trigger: "blur" },
      {
        min: 2,
        max: 100,
        message: "secretKey长度必须介于 2 和 100 之间",
        trigger: "blur",
      },
    ],
    bucketName: [
      { required: true, message: "bucketName不能为空", trigger: "blur" },
      {
        min: 2,
        max: 100,
        message: "bucketName长度必须介于 2 和 100 之间",
        trigger: "blur",
      },
    ],
    endpoint: [
      { required: true, message: "endpoint不能为空", trigger: "blur" },
      {
        min: 2,
        max: 100,
        message: "endpoint名称长度必须介于 2 和 100 之间",
        trigger: "blur",
      },
    ],
    accessPolicy:[
      { required: true, message: "accessPolicy不能为空", trigger: "blur" }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询对象存储配置列表 */
function getList() {
  loading.value = true;
  listOssConfig(queryParams.value).then((response) => {
    ossConfigList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}
/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    ossConfigId: undefined,
    configKey: undefined,
    accessKey: undefined,
    secretKey: undefined,
    bucketName: undefined,
    prefix: undefined,
    endpoint: undefined,
    domain: undefined,
    isHttps: "N",
    accessPolicy: "1",
    region: undefined,
    status: "1",
    remark: undefined,
  };
  proxy.resetForm("ossConfigRef");
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}
/** 选择条数  */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.ossConfigId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}
/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加对象存储配置";
}
/** 修改按钮操作 */
function handleUpdate(row) {
  loading.value = true;
  reset();
  const ossConfigId = row.ossConfigId || ids.value;
  getOssConfig(ossConfigId).then((response) => {
    loading.value = false;
    form.value = response.data;
    open.value = true;
    title.value = "修改对象存储配置";
  });
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["ossConfigRef"].validate(valid => {
    if (valid) {
      buttonLoading.value = true;
      if (form.value.ossConfigId != null) {
        updateOssConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        }).finally(() => {
          buttonLoading.value = false;
        });
      } else {
        addOssConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        }).finally(() => {
          buttonLoading.value = false;
        });
      }
    }
  });
}
/** 用户状态修改  */
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要"' + text + '""' + row.configKey + '"配置吗?').then(() => {
    return changeOssConfigStatus(row.ossConfigId, row.status, row.configKey);
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess(text + "成功");
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0";
  });
}
/** 删除按钮操作 */
function handleDelete(row) {
  const ossConfigIds = row.ossConfigId || ids.value;
  proxy.$modal.confirm('是否确认删除OSS配置编号为"' + ossConfigIds + '"的数据项?').then(() => {
    loading.value = true;
    return delOssConfig(ossConfigIds);
  }).then(() => {
    loading.value = false;
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).finally(() => {
    loading.value = false;
  });
}

getList();
</script>
