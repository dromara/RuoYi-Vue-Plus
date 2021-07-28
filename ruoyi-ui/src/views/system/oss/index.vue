<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文件名" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="原名" prop="originalName">
        <el-input
          v-model="queryParams.originalName"
          placeholder="请输入原名"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文件后缀" prop="fileSuffix">
        <el-input
          v-model="queryParams.fileSuffix"
          placeholder="请输入文件后缀"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="daterangeCreateTime"
          size="small"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="上传人" prop="createBy">
        <el-input
          v-model="queryParams.createBy"
          placeholder="请输入上传人"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="服务商" prop="service">
        <el-input
          v-model="queryParams.service"
          placeholder="请输入服务商"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
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
          @click="handleFile"
          v-hasPermi="['system:oss:upload']"
        >上传文件</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleImage"
          v-hasPermi="['system:oss:upload']"
        >上传图片</el-button>
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

    <el-table v-loading="loading" :data="ossList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="云存储主键" align="center" prop="ossId" v-if="false"/>
      <el-table-column label="文件名" align="center" prop="fileName" />
      <el-table-column label="原名" align="center" prop="originalName" />
      <el-table-column label="文件后缀" align="center" prop="fileSuffix" />
      <el-table-column label="文件展示" align="center" prop="url">
        <template slot-scope="scope">
          <el-image
            v-if="previewListResource && scope.row.fileSuffix.indexOf('png','jpg','jpeg') > 0"
            style="width: 100px; height: 100px;"
            :src="scope.row.url"
            :preview-src-list="[scope.row.url]"/>
          <span v-text="scope.row.url"
                v-if="scope.row.fileSuffix.indexOf('png','jpg','jpeg') < 0 || !previewListResource"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="上传人" align="center" prop="createBy" />
      <el-table-column label="服务商" align="center" prop="service" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleDownload(scope.row)"
            v-hasPermi="['system:oss:download']"
          >下载</el-button>
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
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改OSS云存储对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文件名">
          <fileUpload v-model="form.file" v-if="type === 0"/>
          <imageUpload v-model="form.file" v-if="type === 1"/>
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
import { listOss, delOss } from "@/api/system/oss";
import { downLoadOss } from "@/utils/ossdownload";
import { updateConfig } from "@/api/system/config";


export default {
  name: "Oss",
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
      // OSS云存储表格数据
      ossList: [],
      // 弹出层标题
      title: "",
      // 弹出层标题
      type: 0,
      // 是否显示弹出层
      open: false,
      // 预览列表图片
      previewListResource: true,
      // 创建时间时间范围
      daterangeCreateTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileName: undefined,
        originalName: undefined,
        fileSuffix: undefined,
        url: undefined,
        createTime: undefined,
        createBy: undefined,
        service: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        file: [
          { required: true, message: "文件不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询OSS云存储列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      this.getConfigKey("sys.oss.previewListResource").then(response => {
        this.previewListResource = response.msg === undefined ? true : response.msg === 'true';
      });
      listOss(this.queryParams).then(response => {
        this.ossList = response.rows;
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
        file: undefined,
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
      this.daterangeCreateTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.ossId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 文件按钮操作 */
    handleFile() {
      this.reset();
      this.open = true;
      this.title = "上传文件";
      this.type = 0;
    },
    /** 图片按钮操作 */
    handleImage() {
      this.reset();
      this.open = true;
      this.title = "上传图片";
      this.type = 1;
    },
    /** 提交按钮 */
    submitForm() {
      this.open = false;
      this.getList();
    },
    /** 下载按钮操作 */
    handleDownload(row) {
      downLoadOss(row.ossId)
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ossIds = row.ossId || this.ids;
      this.$confirm('是否确认删除OSS云存储编号为"' + ossIds + '"的数据项?', "警告", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(() => {
          this.loading = true;
          return delOss(ossIds);
        }).then(() => {
          this.loading = false;
          this.getList();
          this.msgSuccess("删除成功");
        }).catch(() => {});
    }
  }
};
</script>
