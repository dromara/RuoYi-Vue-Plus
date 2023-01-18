<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="配置key" prop="configKey">
        <el-input
          v-model="queryParams.configKey"
          placeholder="配置key"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="桶名称" prop="bucketName">
        <el-input
          v-model="queryParams.bucketName"
          placeholder="请输入桶名称"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否默认" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable size="small">
          <el-option key="0" label="是" value="0"/>
          <el-option key="1" label="否" value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:oss:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:oss:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:oss:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ossConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主建" align="center" prop="ossConfigId" v-if="false"/>
      <el-table-column label="配置key" align="center" prop="configKey" />
      <el-table-column label="访问站点" align="center" prop="endpoint" width="200" />
      <el-table-column label="自定义域名" align="center" prop="domain" width="200" />
      <el-table-column label="桶名称" align="center" prop="bucketName" />
      <el-table-column label="前缀" align="center" prop="prefix" />
      <el-table-column label="域" align="center" prop="region" />
      <el-table-column label="桶权限类型" align="center" prop="accessPolicy">
        <template slot-scope="scope">
          <el-tag type="warning" v-if="scope.row.accessPolicy === '0'">private</el-tag>
          <el-tag type="success" v-if="scope.row.accessPolicy === '1'">public</el-tag>
          <el-tag type="info" v-if="scope.row.accessPolicy === '2'">custom</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否默认" align="center" prop="status">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:oss:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:oss:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改对象存储配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
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
              v-for="dict in dict.type.sys_yes_no"
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
      <div slot="footer" class="dialog-footer">
        <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import {
  listOssConfig,
  getOssConfig,
  delOssConfig,
  addOssConfig,
  updateOssConfig,
  changeOssConfigStatus
} from "@/api/system/ossConfig";

export default {
  name: "OssConfig",
  dicts: ['sys_yes_no'],
  data() {
    return {
      // 按钮loading
      buttonLoading: false,
      // 遮罩层
      loading: true,
      // 导出遮罩层
      exportLoading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 对象存储配置表格数据
      ossConfigList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否https字典
      isHttpsOptions: [],
      // 状态(0正常 1停用)字典
      statusOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configKey: undefined,
        bucketName: undefined,
        status: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
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
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询对象存储配置列表 */
    getList() {
      this.loading = true;
      listOssConfig(this.queryParams).then((response) => {
        this.ossConfigList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
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
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.ossConfigId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加对象存储配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.loading = true;
      this.reset();
      const ossConfigId = row.ossConfigId || this.ids;
      getOssConfig(ossConfigId).then((response) => {
        this.loading = false;
        this.form = response.data;
        this.open = true;
        this.title = "修改对象存储配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.ossConfigId != null) {
            updateOssConfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            }).finally(() => {
              this.buttonLoading = false;
            });
          } else {
            addOssConfig(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            }).finally(() => {
              this.buttonLoading = false;
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ossConfigIds = row.ossConfigId || this.ids;
      this.$modal.confirm('是否确认删除对象存储配置编号为"' + ossConfigIds + '"的数据项?').then(() => {
        this.loading = true;
        return delOssConfig(ossConfigIds);
      }).then(() => {
        this.loading = false;
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).finally(() => {
        this.loading = false;
      });
    },
    // 对象存储配置状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.configKey + '"配置吗?').then(() => {
        return changeOssConfigStatus(row.ossConfigId, row.status, row.configKey);
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.status = row.status === "0" ? "1" : "0";
      })
    }
  }
};
</script>
