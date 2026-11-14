<template>
  <div class="p-2 app-container profile-page">
    <el-row :gutter="20" class="profile-grid">
      <el-col :span="6" :xs="24">
        <el-card shadow="hover" class="side-panel profile-summary-card">
          <template #header>
            <div class="panel-heading">
              <div><h3>个人信息</h3></div>
            </div>
          </template>
          <div class="profile-summary">
            <div class="text-center profile-avatar">
              <userAvatar />
            </div>
            <ul class="list-group list-group-striped">
              <li class="list-group-item">
                <div class="profile-item-label">
                  <svg-icon icon-class="user" />
                  <span>用户名称</span>
                </div>
                <span class="profile-item-value">{{ state.user.userName || '-' }}</span>
              </li>
              <li class="list-group-item">
                <div class="profile-item-label">
                  <svg-icon icon-class="phone" />
                  <span>手机号码</span>
                </div>
                <span class="profile-item-value">{{ state.user.phoneNumber || '-' }}</span>
              </li>
              <li class="list-group-item">
                <div class="profile-item-label">
                  <svg-icon icon-class="email" />
                  <span>用户邮箱</span>
                </div>
                <span class="profile-item-value">{{ state.user.email || '-' }}</span>
              </li>
              <li class="list-group-item">
                <div class="profile-item-label">
                  <svg-icon icon-class="tree" />
                  <span>所属部门</span>
                </div>
                <span v-if="state.user.deptName" class="profile-item-value">
                  {{ state.user.deptName }} / {{ state.postGroup }}
                </span>
                <span v-else class="profile-item-value">-</span>
              </li>
              <li class="list-group-item">
                <div class="profile-item-label">
                  <svg-icon icon-class="peoples" />
                  <span>所属角色</span>
                </div>
                <span class="profile-item-value">{{ state.roleGroup || '-' }}</span>
              </li>
              <li class="list-group-item">
                <div class="profile-item-label">
                  <svg-icon icon-class="date" />
                  <span>创建日期</span>
                </div>
                <span class="profile-item-value">{{ state.user.createTime || '-' }}</span>
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18" :xs="24">
        <el-card shadow="hover" class="table-panel profile-main-card">
          <template #header>
            <div class="toolbar-shell">
              <div class="table-heading">
                <h3>个人设置</h3>
                <p>维护个人资料、密码、第三方应用和在线设备。</p>
              </div>
            </div>
          </template>
          <el-tabs v-model="activeTab" class="profile-tabs">
            <el-tab-pane label="基本资料" name="userinfo">
              <userInfo :user="userForm" />
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="resetPwd">
              <resetPwd />
            </el-tab-pane>
            <el-tab-pane label="第三方应用" name="thirdParty">
              <thirdParty :auths="state.auths" />
            </el-tab-pane>
            <el-tab-pane label="在线设备" name="onlineDevice">
              <onlineDevice :devices="state.devices" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Profile" lang="ts">
import { getOnline } from '@/api/monitor/online';
import { getAuthList } from '@/api/system/social/auth';
import { getUserProfile } from '@/api/system/user';
import { UserVO } from '@/api/system/user/types';
import OnlineDevice from './onlineDevice.vue';
import ResetPwd from './resetPwd.vue';
import ThirdParty from './thirdParty.vue';
import UserAvatar from './userAvatar.vue';
import UserInfo from './userInfo.vue';

const activeTab = ref('userinfo');
interface State {
  user: Partial<UserVO>;
  roleGroup: string;
  postGroup: string;
  auths: any;
  devices: any;
}
const state = ref<State>({
  user: {},
  roleGroup: '',
  postGroup: '',
  auths: [],
  devices: []
});

const userForm = ref({});

const getUser = async () => {
  const res = await getUserProfile();
  state.value.user = res.data.user;
  userForm.value = { ...res.data.user };
  state.value.roleGroup = res.data.roleGroup;
  state.value.postGroup = res.data.postGroup;
};

const getAuths = async () => {
  const res = await getAuthList();
  state.value.auths = res.data;
};
const getOnlines = async () => {
  const res = await getOnline();
  state.value.devices = res.data?.rows;
};

onMounted(() => {
  getUser();
  getAuths();
  getOnlines();
});
</script>

<style lang="scss" scoped>
.profile-grid {
  row-gap: 12px;
}

.profile-summary-card,
.profile-main-card {
  height: 100%;
}

.profile-summary {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-avatar {
  margin-bottom: 4px;
}

.profile-item-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.profile-item-value {
  max-width: 58%;
  text-align: right;
  color: var(--el-text-color-secondary);
  word-break: break-word;
}

.profile-summary :deep(.list-group-item) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after),
.profile-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.profile-tabs :deep(.el-tabs__nav) {
  gap: 8px;
}

.profile-tabs :deep(.el-tabs__item) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  min-width: 88px;
  padding: 0 16px;
  border-radius: 10px;
  color: var(--el-text-color-regular);
}

.profile-tabs :deep(.el-tabs__item:nth-child(2)),
.profile-tabs :deep(.el-tabs__item.is-top:first-child),
.profile-tabs :deep(.el-tabs__item.is-top:last-child) {
  padding-left: 16px;
  padding-right: 16px;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  background: rgba(53, 109, 255, 0.1);
  color: var(--el-color-primary);
}

@media (max-width: 768px) {
  .profile-item-value {
    max-width: 50%;
  }
}

.list-group-striped > .list-group-item {
  border-left: 0;
  border-right: 0;
  border-radius: 0;
  padding-left: 0;
  padding-right: 0;
}

.list-group {
  padding-left: 0px;
  list-style: none;
}

.list-group-item {
  border-bottom: 1px solid #e7eaec;
  border-top: 1px solid #e7eaec;
  margin-bottom: -1px;
  padding: 11px 0px;
  font-size: 13px;
}
</style>
