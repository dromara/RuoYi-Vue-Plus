<template>
  <el-form ref="formRef" :model="infoForm" :rules="rules" label-width="150px">
    <el-row>
      <el-col :span="12">
        <el-form-item prop="tplCategory">
          <template #label>生成模板</template>
          <el-select v-model="infoForm.tplCategory">
            <el-option label="单表（增删改查）" value="crud" />
            <el-option label="树表（增删改查）" value="tree" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="packageName">
          <template #label>
            生成包路径
            <el-tooltip content="生成在哪个java包下，例如 com.ruoyi.system" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="infoForm.packageName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="moduleName">
          <template #label>
            生成模块名
            <el-tooltip content="可理解为子系统名，例如 system" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="infoForm.moduleName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="businessName">
          <template #label>
            生成业务名
            <el-tooltip content="可理解为功能英文名，例如 user" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="infoForm.businessName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="functionName">
          <template #label>
            生成功能名
            <el-tooltip content="用作类描述，例如 用户" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="infoForm.functionName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item>
          <template #label>
            上级菜单
            <el-tooltip content="分配到指定菜单下，例如 系统管理" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-tree-select
            v-model="infoForm.parentMenuId"
            :data="menuOptions"
            :props="{ value: 'menuId', label: 'menuName', children: 'children' } as any"
            value-key="menuId"
            node-key="menuId"
            placeholder="选择上级菜单"
            check-strictly
            filterable
            clearable
            highlight-current
          />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="frontendType">
          <template #label>
            前端模板
            <el-tooltip content="对应后端 resources/vm 下的模板目录，例如 vue、react" placement="top">
              <el-icon><question-filled /></el-icon>
            </el-tooltip>
          </template>
          <el-radio-group v-model="infoForm.frontendType">
            <el-radio v-for="item in frontendTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>

    </el-row>

    <h4 class="form-header">增强选项</h4>
    <el-row class="enhance-options" :gutter="20">
      <el-col :span="24">
        <el-row :gutter="20" class="enhance-option-row">
          <el-col :span="8">
            <el-form-item prop="enableExport" class="enhance-toggle-item">
              <template #label>
                导出能力
                <el-tooltip content="关闭后将不生成 export 接口与前端导出按钮" placement="top">
                  <el-icon><question-filled /></el-icon>
                </el-tooltip>
              </template>
              <el-switch v-model="infoForm.enableExport" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-col>

      <el-col :span="24">
        <el-row :gutter="20" class="enhance-option-row">
          <el-col :span="8">
            <el-form-item prop="enableStatus" class="enhance-toggle-item">
              <template #label>
                状态切换
                <el-tooltip content="开启后生成 changeStatus 接口与列表状态开关列" placement="top">
                  <el-icon><question-filled /></el-icon>
                </el-tooltip>
              </template>
              <el-switch v-model="infoForm.enableStatus" />
            </el-form-item>
          </el-col>
          <el-col v-if="infoForm.enableStatus" :span="16">
            <el-form-item prop="statusField">
              <el-select v-model="infoForm.statusField" placeholder="请选择状态字段">
                <el-option
                  v-for="column in availableColumns"
                  :key="column.columnName"
                  :label="column.columnName + '：' + column.columnComment"
                  :value="column.columnName"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-col>

      <el-col :span="24">
        <el-row :gutter="20" class="enhance-option-row">
          <el-col :span="8">
            <el-form-item prop="enableUnique" class="enhance-toggle-item">
              <template #label>
                组合唯一校验
                <el-tooltip content="开启后按选中的字段生成组合唯一校验，新增和修改都会校验" placement="top">
                  <el-icon><question-filled /></el-icon>
                </el-tooltip>
              </template>
              <el-switch v-model="infoForm.enableUnique" />
            </el-form-item>
          </el-col>
          <el-col v-if="infoForm.enableUnique" :span="16">
            <el-form-item prop="uniqueFields">
              <el-select v-model="infoForm.uniqueFields" multiple clearable filterable placeholder="请选择唯一字段">
                <el-option
                  v-for="column in availableColumns"
                  :key="column.columnName"
                  :label="column.columnName + '：' + column.columnComment"
                  :value="column.columnName"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-col>

      <el-col :span="24">
        <el-row :gutter="20" class="enhance-option-row">
          <el-col :span="8">
            <el-form-item prop="enableSort" class="enhance-toggle-item">
              <template #label>
                排序调整
                <el-tooltip content="开启后生成 updateSort 接口，并在列表中以输入框形式快速调整排序字段" placement="top">
                  <el-icon><question-filled /></el-icon>
                </el-tooltip>
              </template>
              <el-switch v-model="infoForm.enableSort" />
            </el-form-item>
          </el-col>
          <el-col v-if="infoForm.enableSort" :span="16">
            <el-form-item prop="sortField">
              <el-select v-model="infoForm.sortField" placeholder="请选择排序字段">
                <el-option
                  v-for="column in sortableColumns"
                  :key="column.columnName"
                  :label="column.columnName + '：' + column.columnComment"
                  :value="column.columnName"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-col>
    </el-row>

    <template v-if="info.tplCategory == 'tree'">
      <h4 class="form-header">其他信息</h4>
      <el-row v-show="info.tplCategory == 'tree'">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树编码字段
              <el-tooltip content="树显示的编码字段名， 如：dept_id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="infoForm.treeCode" placeholder="请选择">
              <el-option
                v-for="(column, index) in availableColumns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树父编码字段
              <el-tooltip content="树显示的父编码字段名， 如：parent_Id" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="infoForm.treeParentCode" placeholder="请选择">
              <el-option
                v-for="(column, index) in availableColumns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              树名称字段
              <el-tooltip content="树节点的显示名称字段名， 如：dept_name" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="infoForm.treeName" placeholder="请选择">
              <el-option
                v-for="(column, index) in availableColumns"
                :key="index"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item prop="treeRootValue">
            <template #label>
              根节点值
              <el-tooltip content="默认是 0，用于根节点 parentId 的默认值" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-input v-model="infoForm.treeRootValue" placeholder="请输入根节点值" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item prop="treeAncestorsField">
            <template #label>
              祖级字段
              <el-tooltip content="选择 ancestors 一类字段后，生成器会自动维护祖级链，避免依赖数据库递归语法" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="infoForm.treeAncestorsField" clearable placeholder="请选择祖级字段">
              <el-option
                v-for="column in availableColumns"
                :key="column.columnName"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item prop="treeOrderField">
            <template #label>
              树排序字段
              <el-tooltip content="树列表默认按祖级、父节点、树排序字段、主键升序排列" placement="top">
                <el-icon><question-filled /></el-icon>
              </el-tooltip>
            </template>
            <el-select v-model="infoForm.treeOrderField" clearable placeholder="请选择树排序字段">
              <el-option
                v-for="column in sortableColumns"
                :key="column.columnName"
                :label="column.columnName + '：' + column.columnComment"
                :value="column.columnName"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </template>

  </el-form>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus';
import { listMenu } from '@/api/system/menu';
import { propTypes } from '@/utils/propTypes';
import { handleTree } from '@/utils/ruoyi';

interface MenuOptionsType {
  menuId: number | string;
  menuName: string;
  children?: MenuOptionsType[];
}

const menuOptions = ref<Array<MenuOptionsType>>([]);
const formRef = ref<FormInstance>();
const frontendTypeOptions = [
  { label: 'Vue', value: 'vue' },
  { label: 'React', value: 'react' }
];

const props = defineProps({
  info: propTypes.any.isRequired,
  columns: propTypes.any.isRequired
});

const infoForm = computed(() => props.info);
const availableColumns = computed(() => props.columns ?? []);
const sortableColumns = computed(() =>
  availableColumns.value.filter((column: any) =>
    ['Integer', 'Long', 'Double', 'BigDecimal', 'LocalDateTime'].includes(column.javaType)
  )
);

// 表单校验
const rules = ref({
  tplCategory: [{ required: true, message: '请选择生成模板', trigger: 'blur' }],
  frontendType: [
    { required: true, message: '请选择前端模板', trigger: 'change' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅支持字母、数字、下划线和中划线', trigger: 'change' }
  ],
  packageName: [{ required: true, message: '请输入生成包路径', trigger: 'blur' }],
  moduleName: [{ required: true, message: '请输入生成模块名', trigger: 'blur' }],
  businessName: [{ required: true, message: '请输入生成业务名', trigger: 'blur' }],
  functionName: [{ required: true, message: '请输入生成功能名', trigger: 'blur' }],
  statusField: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (infoForm.value.enableStatus && !value) {
          callback(new Error('请选择状态字段'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ],
  uniqueFields: [
    {
      validator: (_rule: any, value: string[], callback: (error?: Error) => void) => {
        if (infoForm.value.enableUnique && (!value || value.length === 0)) {
          callback(new Error('请选择唯一字段'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ],
  sortField: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (infoForm.value.enableSort && !value) {
          callback(new Error('请选择排序字段'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ],
  treeRootValue: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (infoForm.value.tplCategory === 'tree' && !value) {
          callback(new Error('请输入根节点值'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
});
/** 查询菜单下拉树结构 */
const getMenuTreeselect = async () => {
  const res = await listMenu();
  const data = handleTree<MenuOptionsType>(res.data, 'menuId');

  if (data) {
    menuOptions.value = data;
  }
};

watch(
  () => infoForm.value.enableStatus,
  val => {
    if (!val) {
      infoForm.value.statusField = '';
    }
  }
);

watch(
  () => infoForm.value.enableUnique,
  val => {
    if (!val) {
      infoForm.value.uniqueFields = [];
    }
  }
);

watch(
  () => infoForm.value.enableSort,
  val => {
    if (!val) {
      infoForm.value.sortField = '';
    }
  }
);

async function validate(): Promise<boolean> {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
}

defineExpose({ validate });

onMounted(() => {
  getMenuTreeselect();
});
</script>

<style scoped>
.enhance-options {
  margin-top: 4px;
}

.enhance-option-row {
  min-height: 52px;
}

.enhance-toggle-item :deep(.el-form-item__content) {
  justify-content: flex-start;
}

@media (max-width: 992px) {
  .enhance-options :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }

  .enhance-option-row {
    min-height: auto;
  }
}
</style>
