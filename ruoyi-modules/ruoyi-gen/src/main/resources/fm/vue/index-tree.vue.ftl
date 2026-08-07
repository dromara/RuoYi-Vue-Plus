<template>
  <div class="p-2 page-shell ${moduleName}-${businessName}-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div><h3>筛选条件</h3></div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
<#list columns as column>
<#if column.query>
<#if column.htmlType == "input" || column.htmlType == "textarea">
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-input v-model="queryParams.${column.javaField}" placeholder="请输入${column.columnLabel}" clearable @keyup.enter="handleQuery" />
            </el-form-item>
<#elseif column.htmlType == "inputNumber">
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-input-number v-model="queryParams.${column.javaField}" controls-position="right" />
            </el-form-item>
<#elseif (column.htmlType == "select" || column.htmlType == "radio") && column.dictType?has_content>
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-select v-model="queryParams.${column.javaField}" placeholder="请选择${column.columnLabel}" clearable>
                <el-option v-for="dict in ${column.dictType}" :key="dict.value" :label="dict.label" :value="dict.value"/>
              </el-select>
            </el-form-item>
<#elseif column.htmlType == "switch" && column.dictType?has_content>
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-select v-model="queryParams.${column.javaField}" placeholder="请选择${column.columnLabel}" clearable>
                <el-option v-for="dict in ${column.dictType}" :key="dict.value" :label="dict.label" :value="dict.value"/>
              </el-select>
            </el-form-item>
<#elseif column.htmlType == "switch">
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-select v-model="queryParams.${column.javaField}" placeholder="请选择${column.columnLabel}" clearable>
<#if column.javaType == "Boolean">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
<#elseif column.javaType == "Integer" || column.javaType == "Long">
                <el-option label="开启" :value="0" />
                <el-option label="关闭" :value="1" />
<#else>
                <el-option label="开启" value="0" />
                <el-option label="关闭" value="1" />
</#if>
              </el-select>
            </el-form-item>
<#elseif (column.htmlType == "select" || column.htmlType == "radio") && !(column.dictType?has_content)>
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-select v-model="queryParams.${column.javaField}" placeholder="请选择${column.columnLabel}" clearable>
                <el-option label="请选择字典生成" value="" />
              </el-select>
            </el-form-item>
<#elseif column.htmlType == "datetime" && column.queryType != "BETWEEN">
            <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
              <el-date-picker clearable
                v-model="queryParams.${column.javaField}"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择${column.columnLabel}"
              />
            </el-form-item>
<#elseif column.htmlType == "datetime" && column.queryType == "BETWEEN">
            <el-form-item label="${column.columnLabel}" style="width: 308px">
              <el-date-picker
                v-model="dateRange${column.capJavaField}"
                value-format="YYYY-MM-DD HH:mm:ss"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
              />
            </el-form-item>
</#if>
</#if>
</#list>
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
            <h3>${functionName}列表</h3>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" plain icon="Plus" @click="handleAdd()" v-hasPermi="['${moduleName}:${businessName}:add']">新增</el-button>
            <el-button type="info" plain icon="Sort" @click="handleToggleExpandAll">展开/折叠</el-button>
<#if enableExport>
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['${moduleName}:${businessName}:export']">导出</el-button>
</#if>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>
      <el-table
        ref="${businessName}TableRef"
        v-loading="loading"
        class="data-table"
        :data="${businessName}List"
        row-key="${treeCode}"
        border
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
<#assign firstTreeListField = "">
<#list columns as tempColumn>
<#if !tempColumn.pk && tempColumn.list && "" != tempColumn.javaField && firstTreeListField == "">
<#assign firstTreeListField = tempColumn.javaField>
</#if>
</#list>
<#list columns as column>
<#if column.pk>
<#elseif enableStatus && statusField == column.javaField>
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}">
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}">
</#if>
          <template #default="scope">
            <el-switch
              v-model="scope.row.${column.javaField}"
              :active-value="${statusField}ActiveValue"
              :inactive-value="${statusField}InactiveValue"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
<#elseif enableSort && sortField == column.javaField>
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}" width="160">
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}" width="160">
</#if>
          <template #default="scope">
<#if column.javaType == "LocalDateTime">
            <el-date-picker
              v-model="scope.row.${column.javaField}"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择${column.columnLabel}"
              @change="handleSortChange(scope.row)"
            />
<#else>
            <el-input-number v-model="scope.row.${column.javaField}" controls-position="right" :min="0" @change="handleSortChange(scope.row)" />
</#if>
          </template>
        </el-table-column>
<#elseif column.list && column.htmlType == "switch">
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}" width="120">
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}" width="120">
</#if>
          <template #default="scope">
            <el-switch
              v-model="scope.row.${column.javaField}"
<#if column.javaType == "Boolean">
              :active-value="true"
              :inactive-value="false"
<#elseif column.javaType == "Integer" || column.javaType == "Long">
              :active-value="0"
              :inactive-value="1"
<#else>
              active-value="0"
              inactive-value="1"
</#if>
              disabled
            />
          </template>
        </el-table-column>
<#elseif column.list && column.htmlType == "datetime">
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}" width="180">
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}" width="180">
</#if>
          <template #default="scope">
            <span>{{ parseTime(scope.row.${column.javaField}, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
<#elseif column.list && column.htmlType == "imageUpload">
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}Url" width="100">
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}Url" width="100">
</#if>
          <template #default="scope">
            <image-preview :src="scope.row.${column.javaField}Url" :width="50" :height="50"/>
          </template>
        </el-table-column>
<#elseif column.list && column.dictColumn>
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}">
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}">
</#if>
          <template #default="scope">
<#if column.htmlType == "checkbox">
            <dict-tag :options="${column.dictType}" :value="scope.row.${column.javaField} ? scope.row.${column.javaField}.split(',') : []"/>
<#else>
            <dict-tag :options="${column.dictType}" :value="scope.row.${column.javaField}"/>
</#if>
          </template>
        </el-table-column>
<#elseif column.list && "" != column.javaField>
<#if column.javaField == firstTreeListField>
        <el-table-column label="${column.columnLabel}" prop="${column.javaField}" />
<#else>
        <el-table-column label="${column.columnLabel}" align="center" prop="${column.javaField}" />
</#if>
</#if>
</#list>
<#if enableStatus && !statusColumn.list>
        <el-table-column label="${statusColumn.columnComment}" align="center" prop="${statusField}">
          <template #default="scope">
            <el-switch
              v-model="scope.row.${statusField}"
              :active-value="${statusField}ActiveValue"
              :inactive-value="${statusField}InactiveValue"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
</#if>
<#if enableSort && !sortColumn.list>
        <el-table-column label="${sortColumn.columnComment}" align="center" prop="${sortField}" width="160">
          <template #default="scope">
<#if sortColumn.javaType == "LocalDateTime">
            <el-date-picker
              v-model="scope.row.${sortField}"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择${sortColumn.columnComment}"
              @change="handleSortChange(scope.row)"
            />
<#else>
            <el-input-number v-model="scope.row.${sortField}" controls-position="right" :min="0" @change="handleSortChange(scope.row)" />
</#if>
          </template>
        </el-table-column>
</#if>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['${moduleName}:${businessName}:edit']" />
            </el-tooltip>
            <el-tooltip content="新增" placement="top">
              <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['${moduleName}:${businessName}:add']" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['${moduleName}:${businessName}:remove']" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 添加或修改${functionName}对话框 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="500px" append-to-body>
      <el-form ref="${businessName}FormRef" :model="form" :rules="rules" label-width="80px">
<#list columns as column>
<#if (column.insert || column.edit) && !column.pk>
<#if "" != treeParentCode && column.javaField == treeParentCode>
        <el-form-item label="${column.columnLabel}" prop="${treeParentCode}">
          <el-tree-select
            v-model="form.${treeParentCode}"
            :data="${businessName}Options"
            :props="{ value: '${treeCode}', label: '${treeName}', children: 'children' } as any"
            value-key="${treeCode}"
            placeholder="请选择${column.columnLabel}"
            check-strictly
          />
        </el-form-item>
<#elseif column.htmlType == "input">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-input v-model="form.${column.javaField}" placeholder="请输入${column.columnLabel}" />
        </el-form-item>
<#elseif column.htmlType == "inputNumber">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-input-number v-model="form.${column.javaField}" controls-position="right" />
        </el-form-item>
<#elseif column.htmlType == "imageUpload">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <image-upload v-model="form.${column.javaField}"/>
        </el-form-item>
<#elseif column.htmlType == "fileUpload">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <file-upload v-model="form.${column.javaField}"/>
        </el-form-item>
<#elseif column.htmlType == "editor">
        <el-form-item label="${column.columnLabel}">
          <editor v-model="form.${column.javaField}" :min-height="192"/>
        </el-form-item>
<#elseif column.htmlType == "select" && column.dictType?has_content>
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-select v-model="form.${column.javaField}" placeholder="请选择${column.columnLabel}">
            <el-option
              v-for="dict in ${column.dictType}"
              :key="dict.value"
              :label="dict.label"
<#if column.javaType == "Integer" || column.javaType == "Long">
              :value="parseInt(dict.value)"
<#else>
              :value="dict.value"
</#if>
            ></el-option>
          </el-select>
        </el-form-item>
<#elseif column.htmlType == "select" && !(column.dictType?has_content)>
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-select v-model="form.${column.javaField}" placeholder="请选择${column.columnLabel}">
            <el-option label="请选择字典生成" value="" />
          </el-select>
        </el-form-item>
<#elseif column.htmlType == "checkbox" && column.dictType?has_content>
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-checkbox-group v-model="form.${column.javaField}">
            <el-checkbox
              v-for="dict in ${column.dictType}"
              :key="dict.value"
              :label="dict.value">
              {{dict.label}}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
<#elseif column.htmlType == "checkbox" && !(column.dictType?has_content)>
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-checkbox-group v-model="form.${column.javaField}">
            <el-checkbox>请选择字典生成</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
<#elseif column.htmlType == "radio" && column.dictType?has_content>
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-radio-group v-model="form.${column.javaField}">
            <el-radio
              v-for="dict in ${column.dictType}"
              :key="dict.value"
<#if column.javaType == "Integer" || column.javaType == "Long">
              :value="parseInt(dict.value)"
<#else>
              :value="dict.value"
</#if>
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
<#elseif column.htmlType == "radio" && !(column.dictType?has_content)>
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-radio-group v-model="form.${column.javaField}">
            <el-radio value="1">请选择字典生成</el-radio>
          </el-radio-group>
        </el-form-item>
<#elseif column.htmlType == "switch">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-switch
            v-model="form.${column.javaField}"
<#if column.javaType == "Boolean">
            :active-value="true"
            :inactive-value="false"
<#elseif column.javaType == "Integer" || column.javaType == "Long">
            :active-value="0"
            :inactive-value="1"
<#else>
            active-value="0"
            inactive-value="1"
</#if>
          />
        </el-form-item>
<#elseif column.htmlType == "datetime">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-date-picker clearable
            v-model="form.${column.javaField}"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择${column.columnLabel}"
          />
        </el-form-item>
<#elseif column.htmlType == "textarea">
        <el-form-item label="${column.columnLabel}" prop="${column.javaField}">
          <el-input v-model="form.${column.javaField}" type="textarea" placeholder="请输入内容" />
        </el-form-item>
</#if>
</#if>
</#list>
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

<script setup name="${BusinessName}" lang="ts">
import {
  add${BusinessName},
<#if enableStatus>
  change${BusinessName}Status,
</#if>
  del${BusinessName},
  get${BusinessName},
  list${BusinessName},
<#if enableSort>
  update${BusinessName}Sort,
</#if>
  update${BusinessName}
} from '@/api/${moduleName}/${businessName}';
import { ${BusinessName}Form, ${BusinessName}Query, ${BusinessName}VO } from '@/api/${moduleName}/${businessName}/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useFormDialog } from '@/hooks/dialog/useFormDialog';
<#if needAddDateRange>
import { useDateRangeQuery } from '@/hooks/form/useDateRangeQuery';
</#if>
import { useSearchReset } from '@/hooks/form/useSearchReset';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import { useTreeTableExpand } from '@/hooks/tree/useTreeTableExpand';
<#if needDict>
import { useDict } from '@/utils/dict';
</#if>
import modal from '@/plugins/modal';
import { handleTree<#if needParseTime>, parseTime</#if> } from '@/utils/ruoyi';
<#if enableExport>
import { download as requestDownload } from '@/utils/request';
</#if>

<#if needDict>
const { ${dictsNoSymbol} } = toRefs<any>(useDict(${dicts}));
</#if>

<#if enableStatus>
const ${statusField}ActiveValue = <#if statusColumn.javaType == "Boolean">true<#elseif statusColumn.javaType == "Integer" || statusColumn.javaType == "Long">0<#else>'0'</#if>;
const ${statusField}InactiveValue = <#if statusColumn.javaType == "Boolean">false<#elseif statusColumn.javaType == "Integer" || statusColumn.javaType == "Long">1<#else>'1'</#if>;
</#if>

type ${BusinessName}Option = {
  ${treeCode}: <#if treeParentColumn.javaType == 'String'>string<#else> number</#if>;
  ${treeName}: string;
  children?: ${BusinessName}Option[];
};

const ${businessName}List = ref<${BusinessName}VO[]>([]);
const ${businessName}Options = ref<${BusinessName}Option[]>([]);
const all${BusinessName}Options = ref<${BusinessName}Option[]>([]);
const buttonLoading = ref(false);
const { showSearch } = useSearchToggle();
const { loading, setLoading, withLoading } = useLoading();

const queryFormRef = ref<ElFormInstance>();
const ${businessName}FormRef = ref<ElFormInstance>();
const ${businessName}TableRef = ref<ElTableInstance>();
const { isExpandAll, handleToggleExpandAll } = useTreeTableExpand<${BusinessName}VO>({
  tableRef: ${businessName}TableRef,
  data: ${businessName}List
});

<#list columns as column>
<#if column.htmlType == "datetime" && column.queryType == "BETWEEN">
const {
  dateRange: dateRange${column.capJavaField},
  applyDateRange: apply${column.capJavaField}DateRange,
  resetDateRange: reset${column.capJavaField}DateRange
} = useDateRangeQuery('${column.capJavaField}');
</#if>
</#list>

const initFormData: ${BusinessName}Form = {
<#list columns as column>
<#if column.insert || column.edit>
<#if column.htmlType == "checkbox">
    ${column.javaField}: [],
<#else>
    ${column.javaField}: undefined,
</#if>
</#if>
</#list>
}

const data = reactive<PageData<${BusinessName}Form, ${BusinessName}Query>>({
  form: {...initFormData},
  queryParams: {
<#list columns as column>
<#if column.query>
<#if column.htmlType != "datetime" || column.queryType != "BETWEEN">
    ${column.javaField}: undefined,
</#if>
</#if>
</#list>
    params: {
<#list columns as column>
<#if column.query>
<#if column.htmlType == "datetime" && column.queryType == "BETWEEN">
      ${column.javaField}: undefined,
</#if>
</#if>
</#list>
    }
  },
  rules: {
<#list columns as column>
<#if column.insert || column.edit>
<#if column.required>
${column.javaField}: [
      { required: true, message: "${column.columnLabel}不能为空", trigger: <#if column.htmlType == "select" || column.htmlType == "radio" || column.htmlType == "switch" || column.htmlType == "inputNumber">"change"<#else>"blur"</#if> }
    ],
</#if>
</#if>
</#list>
  }
});

const { queryParams, form, rules } = toRefs(data);
const { dialog, resetForm: reset, openDialog, showDialog, closeDialog } = useFormDialog({
  form,
  formRef: ${businessName}FormRef,
  initialFormData: initFormData
});

/** 查询${functionName}列表 */
const getList = async () => {
  await withLoading(async () => {
<#if needAddDateRange>
    let params = queryParams.value;
<#list columns as column>
<#if column.htmlType == "datetime" && column.queryType == "BETWEEN">
params = apply${column.capJavaField}DateRange(params);
</#if>
</#list>
    const res = await list${BusinessName}(params);
<#else>
    const res = await list${BusinessName}(queryParams.value);
</#if>
    const data = handleTree<${BusinessName}VO>(res.data, '${treeCode}', '${treeParentCode}');
    if (data) {
      ${businessName}List.value = data;
    }
  });
};

/** 查询${functionName}下拉树结构 */
const getTreeselect = async (excludeId?: string | number) => {
  const res = await list${BusinessName}();
  const data: ${BusinessName}Option = { ${treeCode}: ${treeRootValueTsLiteral}, ${treeName}: '顶级节点', children: [] };
  data.children = handleTree<${BusinessName}Option>(res.data, '${treeCode}', '${treeParentCode}');
  all${BusinessName}Options.value = [data];
  ${businessName}Options.value = excludeId != null ? filterTreeOptions(all${BusinessName}Options.value, excludeId) : all${BusinessName}Options.value;
};

/** 取消按钮 */
const cancel = () => {
  reset();
  closeDialog();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  getList();
};

const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  resetExtras: () => {
<#list columns as column>
<#if column.htmlType == "datetime" && column.queryType == "BETWEEN">
reset${column.capJavaField}DateRange();
</#if>
</#list>
  },
  afterReset: () => {
    handleQuery();
  }
});

/** 新增按钮操作 */
const handleAdd = (row?: Partial<${BusinessName}VO>) => {
  openDialog('添加${functionName}');
  getTreeselect();
  if (row != null && row.${treeCode}) {
    form.value.${treeParentCode} = row.${treeCode};
  } else {
    form.value.${treeParentCode} = ${treeRootValueTsLiteral};
  }
};

/** 修改按钮操作 */
const handleUpdate = async (row: Partial<${BusinessName}VO>) => {
  reset();
  await getTreeselect(row.${treeCode});
  if (row != null) {
    form.value.${treeParentCode} = row.${treeParentCode};
  }
  const res = await get${BusinessName}(row.${pkColumn.javaField});
  Object.assign(form.value, res.data);
<#list columns as column>
  <#if column.htmlType == "checkbox">
  form.value.${column.javaField} = form.value.${column.javaField}.split(",");
  </#if>
</#list>
  showDialog('修改${functionName}');
};

/** 提交按钮 */
const submitForm = () => {
  ${businessName}FormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      buttonLoading.value = true;
<#list columns as column>
<#if column.htmlType == "checkbox">
      form.value.${column.javaField} = form.value.${column.javaField}.join(",");
</#if>
</#list>
      if (form.value.${pkColumn.javaField}) {
        await update${BusinessName}(form.value).finally(() => (buttonLoading.value = false));
      } else {
        await add${BusinessName}(form.value).finally(() => (buttonLoading.value = false));
      }
      modal.msgSuccess('操作成功');
      closeDialog();
      await getList();
    }
  });
};

/** 删除按钮操作 */
const handleDelete = async (row: Partial<${BusinessName}VO>) => {
  await modal.confirm('是否确认删除${functionName}编号为"' + row.${pkColumn.javaField} + '"的数据项？');
  setLoading(true);
  await del${BusinessName}(row.${pkColumn.javaField}).finally(() => setLoading(false));
  await getList();
  modal.msgSuccess('删除成功');
};

const filterTreeOptions = (options: ${BusinessName}Option[], excludeId: string | number): ${BusinessName}Option[] => {
  return options
    .filter(item => item.${treeCode} !== excludeId)
    .map(item => ({
      ...item,
      children: item.children ? filterTreeOptions(item.children, excludeId) : []
    }));
};

<#if enableStatus>
/** 状态修改 */
const handleStatusChange = async (row: Partial<${BusinessName}VO>) => {
  const text = row.${statusField} === ${statusField}ActiveValue ? '启用' : '停用';
  try {
    await modal.confirm('确认要"' + text + '"吗?');
    await change${BusinessName}Status(row.${pkColumn.javaField}, row.${statusField});
    modal.msgSuccess(text + '成功');
  } catch (err) {
    row.${statusField} = row.${statusField} === ${statusField}ActiveValue ? ${statusField}InactiveValue : ${statusField}ActiveValue;
  }
};
</#if>

<#if enableSort>
/** 排序调整 */
const handleSortChange = async (row: Partial<${BusinessName}VO>) => {
  try {
    await update${BusinessName}Sort(row.${pkColumn.javaField}, row.${sortField});
    modal.msgSuccess('排序更新成功');
  } catch (err) {
    await getList();
  }
};
</#if>

<#if enableExport>
/** 导出按钮操作 */
const handleExport = () => {
  requestDownload(
    '${moduleName}/${businessName}/export',
    {
      ...queryParams.value
    },
    `${businessName}_${r'${new Date().getTime()}'}.xlsx`
  );
};
</#if>

onMounted(() => {
  getList();
});
</script>
