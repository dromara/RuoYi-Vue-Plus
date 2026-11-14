<template>
  <el-drawer
    v-model="visible"
    title="用户信息详情"
    :size="680"
    append-to-body
    :close-on-click-modal="true"
    @closed="info = null"
  >
    <el-descriptions v-if="info" v-loading="loading" :column="2" border>
      <el-descriptions-item label="用户名称">{{ info.nickName }}</el-descriptions-item>
      <el-descriptions-item label="归属部门">
        {{ info.deptName || (info.dept && info.dept.deptName) || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="手机号码">{{ info.phoneNumber || '-' }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ info.email || '-' }}</el-descriptions-item>
      <el-descriptions-item label="登录账号">{{ info.userName }}</el-descriptions-item>
      <el-descriptions-item label="用户状态">
        <el-tag :type="info.status === '0' ? 'success' : 'danger'">
          {{ selectDictLabel(sys_normal_disable, info.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="岗位">{{ postNames || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户性别">
        {{ selectDictLabel(sys_user_gender, info.gender) || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="角色" :span="2">{{ roleNames || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />

    <el-descriptions v-if="info" :column="2" border>
      <el-descriptions-item label="创建时间">{{ info.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ info.updateTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最后登录IP">{{ info.loginIp || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最后登录时间">{{ info.loginDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ info.remark || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup lang="ts">
import type { PostVO } from '@/api/system/post/types';
import type { RoleVO } from '@/api/system/role/types';
import type { UserVO } from '@/api/system/user/types';
import api from '@/api/system/user';
import { useDict } from '@/utils/dict';
import { selectDictLabel } from '@/utils/ruoyi';

const { sys_normal_disable, sys_user_gender } = toRefs<any>(useDict('sys_normal_disable', 'sys_user_gender'));

const visible = ref(false);
const loading = ref(false);
const info = ref<UserVO | null>(null);
const postOptions = ref<PostVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const postIds = ref<string[]>([]);
const roleIds = ref<string[]>([]);

const postNames = computed(() => {
  if (!postOptions.value.length || !postIds.value.length) return '';
  return postOptions.value
    .filter(p => postIds.value.includes(String(p.postId)))
    .map(p => p.postName)
    .join('、');
});

const roleNames = computed(() => {
  if (!roleOptions.value.length || !roleIds.value.length) return '';
  return roleOptions.value
    .filter(r => roleIds.value.includes(String(r.roleId)))
    .map(r => r.roleName)
    .join('、');
});

async function openDrawer(userId: string | number) {
  visible.value = true;
  loading.value = true;
  try {
    const { data } = await api.getUser(userId);
    info.value = data.user;
    postOptions.value = data.posts || [];
    roleOptions.value = data.roles || [];
    postIds.value = (data.postIds || []).map(String);
    roleIds.value = (data.roleIds || []).map(String);
  } catch (error) {
    console.error('获取用户信息失败:', error);
  } finally {
    loading.value = false;
  }
}

defineExpose({ openDrawer });
</script>

<style lang="scss" scoped>
:deep(.el-descriptions__label) {
  min-width: 110px;
}
</style>
