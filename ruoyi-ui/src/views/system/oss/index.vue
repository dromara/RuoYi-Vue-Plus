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
          value-format="yyyy-MM-dd HH:mm:ss"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="['00:00:00', '23:59:59']"
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
      <el-col :span="1.5">
        <el-button
          :type="previewListResource ? 'danger' : 'warning'"
          plain
          size="mini"
          @click="handlePreviewListResource(!previewListResource)"
          v-hasPermi="['system:oss:edit']"
        >预览开关 : {{previewListResource ? "禁用" : "启用"}}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-s-operation"
          size="mini"
          @click="handleOssConfig"
          v-hasPermi="['system:oss:list']"
        >配置管理</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ossList" @selection-change="handleSelectionChange"
              :header-cell-class-name="handleHeaderClass"
              @header-click="handleHeaderCLick"
              v-if="showTable">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="对象存储主键" align="center" prop="ossId" v-if="false"/>
      <el-table-column label="文件名" align="center" prop="fileName" />
      <el-table-column label="原名" align="center" prop="originalName" />
      <el-table-column label="文件后缀" align="center" prop="fileSuffix" />
      <el-table-column label="文件展示" align="center" prop="url">
        <template slot-scope="scope">
          <ImagePreview
            v-if="previewListResource && checkFileSuffix(scope.row.fileSuffix)"
            :width=100 :height=100
            :src="scope.row.url"
            :preview-src-list="[scope.row.url]"/>
          <span v-text="scope.row.url"
                v-if="!checkFileSuffix(scope.row.fileSuffix) || !previewListResource"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180"
                       sortable="custom">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="上传人" align="center" prop="createBy" />
      <el-table-column label="服务商" align="center" prop="service"
                       sortable="custom"/>
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

    <!-- 添加或修改OSS对象存储对话框 -->
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

export default {
  name: "Oss",
  data() {
    return {
      showTable: true,
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
      // OSS对象存储表格数据
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
      // 默认排序
      defaultSort: {prop: 'createTime', order: 'ascending'},
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
    /** 查询OSS对象存储列表 */
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
        this.showTable = true;
      });
    },
    checkFileSuffix(fileSuffix) {
      let arr = ["png", "jpg", "jpeg"];
      return arr.some(type => {
        return fileSuffix.indexOf(type) > -1;
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
      this.showTable = false;
      this.daterangeCreateTime = [];
      this.resetForm("queryForm");
      this.queryParams.orderByColumn = this.defaultSort.prop;
      this.queryParams.isAsc = this.defaultSort.order;
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.ossId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    // 设置列的排序为我们自定义的排序
    handleHeaderClass({column}) {
      column.order = column.multiOrder
    },
    // 点击表头进行排序
    handleHeaderCLick(column) {
      if (column.sortable !== 'custom') {
        return
      }
      switch (column.multiOrder) {
        case 'descending':
          column.multiOrder = 'ascending';
          break;
        case 'ascending':
          column.multiOrder = '';
          break;
        default:
          column.multiOrder = 'descending';
          break;
      }
      this.handleOrderChange(column.property, column.multiOrder)
    },
    handleOrderChange(prop, order) {
      let orderByArr = this.queryParams.orderByColumn ? this.queryParams.orderByColumn.split(",") : [];
      let isAscArr = this.queryParams.isAsc ? this.queryParams.isAsc.split(",") : [];
      let propIndex = orderByArr.indexOf(prop)
      if (propIndex !== -1) {
        if (order) {
          //排序里已存在 只修改排序
          isAscArr[propIndex] = order;
        } else {
          //如果order为null 则删除排序字段和属性
          isAscArr.splice(propIndex, 1);//删除排序
          orderByArr.splice(propIndex, 1);//删除属性
        }
      } else {
        //排序里不存在则新增排序
        orderByArr.push(prop);
        isAscArr.push(order);
      }
      //合并排序
      this.queryParams.orderByColumn = orderByArr.join(",");
      this.queryParams.isAsc = isAscArr.join(",");
      this.getList();
    },
    /** 任务日志列表查询 */
    handleOssConfig() {
      this.$router.push({ path: '/system/oss-config/index'})
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
      this.$download.oss(row.ossId)
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ossIds = row.ossId || this.ids;
      this.$modal.confirm('是否确认删除OSS对象存储编号为"' + ossIds + '"的数据项?').then(() => {
        this.loading = true;
        return delOss(ossIds);
      }).then(() => {
        this.loading = false;
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).finally(() => {
        this.loading = false;
      });
    },
    // 预览列表图片状态修改
    handlePreviewListResource(previewListResource) {
      let text = previewListResource ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""预览列表图片"配置吗?').then(() => {
        return this.updateConfigByKey("sys.oss.previewListResource", previewListResource);
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        this.previewListResource = previewListResource !== true;
      })
    }
  }
};
</script>
