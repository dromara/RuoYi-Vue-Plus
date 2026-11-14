<template>
  <div class="profile-auth">
    <el-table :data="auths" border class="data-table profile-auth-table">
      <el-table-column label="序号" width="50" type="index" />
      <el-table-column label="绑定账号平台" width="140" align="center" prop="source" show-overflow-tooltip />
      <el-table-column label="头像" width="120" align="center" prop="avatar">
        <template #default="scope">
          <img :src="scope.row.avatar" style="width: 45px; height: 45px" />
        </template>
      </el-table-column>
      <el-table-column label="系统账号" width="180" align="center" prop="userName" :show-overflow-tooltip="true" />
      <el-table-column label="绑定时间" width="180" align="center" prop="createTime" />
      <el-table-column label="操作" width="80" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="解绑" placement="top">
            <el-button link type="primary" icon="CircleClose" @click="unlockAuth(scope.row)"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <div class="provider-section">
      <div class="provider-heading">
        <h4 class="provider-desc">可绑定的第三方应用</h4>
        <p>点击下方平台完成账号绑定。</p>
      </div>
      <div class="user-bind">
        <a class="third-app" href="#" title="使用 微信 账号授权登录" @click="authUrl('wechat')">
          <div class="third-app__icon">
            <svg-icon icon-class="wechat" />
          </div>
          <span class="app-name">微信</span>
        </a>
        <a class="third-app" href="#" title="使用 MaxKey 账号授权登录" @click="authUrl('maxkey')">
          <div class="third-app__icon">
            <svg-icon icon-class="maxkey" />
          </div>
          <span class="app-name">MaxKey</span>
        </a>
        <a class="third-app" href="#" title="使用 TopIam 账号授权登录" @click="authUrl('topiam')">
          <div class="third-app__icon">
            <svg-icon icon-class="topiam" />
          </div>
          <span class="app-name">TopIam</span>
        </a>
        <a class="third-app" href="#" title="使用 Gitee 账号授权登录" @click="authUrl('gitee')">
          <div class="third-app__icon">
            <svg-icon icon-class="gitee" />
          </div>
          <span class="app-name">Gitee</span>
        </a>
        <a class="third-app" href="#" title="使用 GitHub 账号授权登录" @click="authUrl('github')">
          <div class="third-app__icon">
            <svg-icon icon-class="github" />
          </div>
          <span class="app-name">GitHub</span>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { authUnlock, authRouterUrl } from '@/api/system/social/auth';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  auths: propTypes.any.isRequired
});
const auths = computed(() => props.auths);

const unlockAuth = (row: any) => {
  ElMessageBox.confirm('您确定要解除"' + row.source + '"的账号绑定吗？')
    .then(() => {
      return authUnlock(row.id);
    })
    .then((res: any) => {
      if (res.code === 200) {
        modal.msgSuccess('解绑成功');
        tab.refreshPage();
      } else {
        modal.msgError(res.msg);
      }
    })
    .catch(() => {});
};

const authUrl = (source: string) => {
  authRouterUrl(source).then((res: any) => {
    if (res.code === 200) {
      window.location.href = res.data;
    } else {
      modal.msgError(res.msg);
    }
  });
};
</script>

<style lang="scss" scoped>
.profile-auth {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-auth-table {
  width: 100%;
}

.provider-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.provider-heading p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
}

.user-bind .third-app {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 84px;
  padding: 12px 10px;
  border: 1px solid var(--app-surface-border);
  border-radius: 12px;
  background: var(--app-elevated-soft-bg);
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.user-bind {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 108px));
  gap: 10px;
  justify-content: start;
}

.third-app:hover {
  transform: translateY(-1px);
  border-color: rgba(53, 109, 255, 0.3);
  box-shadow: var(--app-shadow-sm);
}

.third-app__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(53, 109, 255, 0.08);
  font-size: 18px;
}

a {
  text-decoration: none;
  cursor: pointer;
  color: inherit;
}

.provider-desc {
  margin: 0;
  font-size: 16px;
}

td > img {
  height: 20px;
  width: 20px;
  display: inline-block;
  border-radius: 50%;
  margin-right: 5px;
}

.app-name {
  font-weight: 600;
  font-size: 13px;
  line-height: 1.2;
  text-align: center;
  word-break: break-word;
}

html.dark {
  .user-bind .third-app {
    background: rgba(15, 23, 42, 0.64);
  }
}
</style>
