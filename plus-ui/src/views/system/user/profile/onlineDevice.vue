<template>
  <div class="profile-table-wrap">
    <el-table :data="devices" border class="data-table profile-device-table">
      <el-table-column label="设备类型" align="center">
        <template #default="scope">
          <dict-tag :options="sys_device_type" :value="scope.row.deviceType" />
        </template>
      </el-table-column>
      <el-table-column label="主机" align="center" prop="ipaddr" :show-overflow-tooltip="true" />
      <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true" />
      <el-table-column label="操作系统" align="center" prop="os" :show-overflow-tooltip="true" />
      <el-table-column label="浏览器" align="center" prop="browser" :show-overflow-tooltip="true" />
      <el-table-column label="登录时间" align="center" prop="loginTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.loginTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handldDelOnline(scope.row)"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup name="Online" lang="ts">
import { delOnline } from '@/api/monitor/online';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';
import { useDict } from '@/utils/dict';
import { propTypes } from '@/utils/propTypes';
import { parseTime } from '@/utils/ruoyi';

const { sys_device_type } = toRefs<any>(useDict('sys_device_type'));

const props = defineProps({
  devices: propTypes.any.isRequired
});
const devices = computed(() => props.devices);

/** 删除按钮操作 */
const handldDelOnline = (row: any) => {
  ElMessageBox.confirm('删除设备后，在该设备登录需要重新进行验证')
    .then(() => {
      return delOnline(row.tokenId);
    })
    .then((res: any) => {
      if (res.code === 200) {
        modal.msgSuccess('删除成功');
        tab.refreshPage();
      } else {
        modal.msgError(res.msg);
      }
    })
    .catch(() => {});
};
</script>

<style lang="scss" scoped>
.profile-table-wrap {
  width: 100%;
}
</style>
