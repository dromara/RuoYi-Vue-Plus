<template>
  <el-dialog
    v-model="open"
    title="操作日志详细"
    width="700px"
    append-to-body
    close-on-click-modal
    @closed="info = null"
  >
    <el-descriptions v-if="info" :column="1" border>
      <el-descriptions-item label="操作状态">
        <template #default>
          <el-tag v-if="info.status === 0" type="success">正常</el-tag>
          <el-tag v-else-if="info.status === 1" type="danger">失败</el-tag>
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="登录信息">
        <template #default>
          {{ info.operName }} / {{ info.deptName }} / {{ info.clientKey || '-' }} /
          {{ deviceTypeFormat(info.deviceType) || '-' }} / {{ info.browser || '-' }} / {{ info.os || '-' }} /
          {{ info.operIp }} / {{ info.operLocation }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="请求信息">
        <template #default>{{ info.requestMethod }} {{ info.operUrl }}</template>
      </el-descriptions-item>
      <el-descriptions-item label="操作模块">
        <template #default>{{ info.title }} / {{ typeFormat(info) }}</template>
      </el-descriptions-item>
      <el-descriptions-item label="操作方法">
        <template #default>
          {{ info.method }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="请求参数">
        <template #default>
          <div class="max-h-300px overflow-y-auto">
            <VueJsonPretty :data="formatToJsonObject(info.operParam)" />
          </div>
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="返回参数">
        <template #default>
          <div class="max-h-300px overflow-y-auto">
            <VueJsonPretty :data="formatToJsonObject(info.jsonResult)" />
          </div>
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="消耗时间">
        <template #default>
          <span>{{ info.costTime }}ms</span>
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="操作时间">
        <template #default>{{ parseTime(info.operTime) }}</template>
      </el-descriptions-item>
      <el-descriptions-item v-if="info.status === 1" label="异常信息">
        <template #default>
          <span class="text-danger">{{ info.errorMsg }}</span>
        </template>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import VueJsonPretty from 'vue-json-pretty';
import type { OperLogForm } from '@/api/monitor/operlog/types';
import 'vue-json-pretty/lib/styles.css';
import { useDict } from '@/utils/dict';
import { parseTime, selectDictLabel } from '@/utils/ruoyi';

const open = ref(false);
const info = ref<OperLogForm | null>(null);
function openDialog(row: OperLogForm) {
  info.value = row;
  open.value = true;
}

function closeDialog() {
  open.value = false;
}

defineExpose({
  openDialog,
  closeDialog
});

/**
 * json转为对象
 * @param data 原始数据
 */
function formatToJsonObject(data: string) {
  try {
    return JSON.parse(data);
  } catch (error) {
    return data;
  }
}

/**
 * 字典信息
 */
const { sys_oper_type, sys_device_type } = toRefs<any>(useDict('sys_oper_type', 'sys_device_type'));
const typeFormat = (row: OperLogForm) => {
  return selectDictLabel(sys_oper_type.value, row.businessType);
};
const deviceTypeFormat = (deviceType: string) => {
  return selectDictLabel(sys_device_type.value, deviceType);
};
</script>

<style lang="scss" scoped>
/**
label宽度固定
*/
:deep(.el-descriptions__label) {
  min-width: 100px;
}
/**
文字超过 换行显示
*/
:deep(.el-descriptions__content) {
  max-width: 300px;
}
</style>
