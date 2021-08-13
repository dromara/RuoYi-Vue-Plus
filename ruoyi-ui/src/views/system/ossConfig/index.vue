<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
      :inline="true"
      v-show="showSearch"
      label-width="100px"
    >
      <el-form-item label="configKey" prop="configKey">
        <el-input
          v-model="queryParams.configKey"
          placeholder="请输入configKey"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="bucketName" prop="bucketName">
        <el-input
          v-model="queryParams.bucketName"
          placeholder="请输入bucketName"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery"
          >重置</el-button
        >
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
          v-hasPermi="['system:sysOssConfig:add']"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:sysOssConfig:edit']"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          :loading="exportLoading"
          @click="handleExport"
          v-hasPermi="['system:sysOssConfig:export']"
          >导出</el-button
        >
      </el-col>
      <right-toolbar
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="sysOssConfigList">
      <el-table-column
        label="主建"
        align="center"
        prop="ossConfigId"
        v-if="false"
      />
      <el-table-column label="configKey" align="center" prop="configKey" />
      <el-table-column label="accessKey" align="center" prop="accessKey" />
      <el-table-column label="seretKey" align="center" prop="secretKey" />
      <el-table-column label="bucketName" align="center" prop="bucketName" />
      <el-table-column
        label="endpoint"
        width="200"
        align="center"
        prop="endpoint"
      />
      <el-table-column label="前缀" align="center" prop="prefix" />
      <el-table-column
        label="是否htpps"
        align="center"
        prop="isHttps"
        :formatter="isHttpsFormat"
      />
      <el-table-column label="region" align="center" prop="region" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:sysOssConfig:edit']"
            >修改</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:sysOssConfig:remove']"
            >删除</el-button
          >
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

    <!-- 添加或修改云存储配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="configKey" prop="configKey">
          <el-row>
            <el-col :span="12">
              <el-select v-model="form.configKey" placeholder="请选择">
                <el-option
                  v-for="item in configKeyOptions"
                  :key="item.configKey"
                  :label="item.label"
                  :value="item.configKey"
                >
                  <span style="float: left">{{ item.configKey }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{
                    item.label
                  }}</span>
                </el-option>
              </el-select>
            </el-col>
            <el-col :span="12">
              <el-form-item label="bucketName" prop="bucketName">
                <el-input
                  v-model="form.bucketName"
                  placeholder="请输入bucketName"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="accessKey" prop="accessKey">
              <el-input
                v-model="form.accessKey"
                placeholder="请输入accessKey"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="secretKey" prop="secretKey">
              <el-input
                v-model="form.secretKey"
                placeholder="请输入secretKey"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="endpoint" prop="endpoint">
          <el-input v-model="form.endpoint" placeholder="请输入endpoint" />
        </el-form-item>
        <el-form-item label="前缀" prop="prefix">
          <el-input v-model="form.prefix" placeholder="请输入前缀" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio label="0">启用</el-radio>
                <el-radio label="1">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否htpps">
              <el-radio-group v-model="form.isHttps">
                <el-radio label="0">否</el-radio>
                <el-radio label="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="region" prop="region">
          <el-input v-model="form.region" placeholder="请输入region" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :loading="buttonLoading" type="primary" @click="submitForm"
          >确 定</el-button
        >
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listSysOssConfig,
  getSysOssConfig,
  delSysOssConfig,
  addSysOssConfig,
  updateSysOssConfig,
  changeOssConfigStatus,
} from "@/api/system/sysOssConfig";
import { downLoadExcel } from "@/utils/download";

export default {
  name: "SysOssConfig",
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
      // 云存储配置表格数据
      sysOssConfigList: [],
      //configKeyOptions
      configKeyOptions: [],
      configKeyDatas: [
        { configKey: "monio", label: "minio" },
        { configKey: "qcloud", label: "腾讯云" },
        { configKey: "aliyun", label: "阿里云" },
        { configKey: "qiniu", label: "七牛云" },
      ],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configKey: undefined,
        bucketName: undefined,
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
      },
    };
  },
  created() {
    this.getList();
    this.configKeyOptions = this.configKeyDatas;
  },
  methods: {
    /** 查询云存储配置列表 */
    getList() {
      this.loading = true;
      listSysOssConfig(this.queryParams).then((response) => {
        this.sysOssConfigList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 是否htpps字典翻译
    isHttpsFormat(row, column) {
      return row.isHttps == "0" ? "否" : "是";
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
        isHttps: "0",
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
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加云存储配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.loading = true;
      this.reset();
      const ossConfigId = row.ossConfigId || this.ids;
      getSysOssConfig(ossConfigId).then((response) => {
        this.loading = false;
        this.form = response.data;
        this.open = true;
        this.title = "修改云存储配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.ossConfigId != null) {
            updateSysOssConfig(this.form)
              .then((response) => {
                this.msgSuccess("修改成功");
                this.open = false;
                this.getList();
              })
              .finally(() => {
                this.buttonLoading = false;
              });
          } else {
            addSysOssConfig(this.form)
              .then((response) => {
                this.msgSuccess("新增成功");
                this.open = false;
                this.getList();
              })
              .finally(() => {
                this.buttonLoading = false;
              });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ossConfigIds = row.ossConfigId || this.ids;
      this.$confirm(
        '是否确认删除configKey为"' + row.configKey + '"的数据项?',
        "警告",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      )
        .then(() => {
          return delSysOssConfig(ossConfigIds);
        })
        .then(() => {
          this.loading = false;
          this.getList();
          this.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      downLoadExcel("/system/sysOssConfig/export", this.queryParams);
    },
    // 云存储配置状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$confirm(
        '确认要"' + text + '""' + row.configKey + '"配置吗?',
        "警告",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      )
        .then(function () {
          return changeOssConfigStatus(
            row.ossConfigId,
            row.status,
            row.configKey
          );
        })
        .then(() => {
          this.msgSuccess(text + "成功");
        })
        .catch(function () {
          row.status = row.status === "0" ? "1" : "0";
        });
    },
  },
};
</script>
