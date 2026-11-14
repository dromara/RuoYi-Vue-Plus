<template>
  <el-form ref="pwdRef" :model="user" :rules="rules" label-width="80px" class="profile-form">
    <el-form-item label="旧密码" prop="oldPassword">
      <el-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" show-password />
    </el-form-item>
    <el-form-item label="新密码" prop="newPassword">
      <el-input v-model="user.newPassword" placeholder="请输入新密码" type="password" show-password />
    </el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password />
    </el-form-item>
    <el-form-item class="profile-form__actions">
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ResetPwdForm } from '@/api/system/user/types';
import { updateUserPwd } from '@/api/system/user';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';

const pwdRef = ref<ElFormInstance>();
const user = ref<ResetPwdForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (user.value.newPassword !== value) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};
const rules = ref({
  oldPassword: [{ required: true, message: '旧密码不能为空', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    {
      min: 6,
      max: 20,
      message: '长度在 6 到 20 个字符',
      trigger: 'blur'
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: '不能包含非法字符：< > " \' \\ |',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    {
      required: true,
      validator: equalToPassword,
      trigger: 'blur'
    }
  ]
});

/** 提交按钮 */
const submit = () => {
  pwdRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await updateUserPwd(user.value.oldPassword, user.value.newPassword);
      modal.msgSuccess('修改成功');
    }
  });
};
/** 关闭按钮 */
const close = () => {
  tab.closePage();
};
</script>

<style lang="scss" scoped>
.profile-form {
  max-width: 520px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.profile-form :deep(.el-button) {
  border-radius: 10px;
}

.profile-form__actions :deep(.el-form-item__content) {
  display: flex;
  gap: 8px;
}
</style>
