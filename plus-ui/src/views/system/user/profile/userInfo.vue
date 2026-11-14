<template>
  <el-form ref="userRef" :model="userForm" :rules="rules" label-width="80px" class="profile-form">
    <el-form-item label="用户昵称" prop="nickName">
      <el-input v-model="userForm.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item label="手机号码" prop="phoneNumber">
      <el-input v-model="userForm.phoneNumber" maxlength="11" />
    </el-form-item>
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="userForm.email" maxlength="50" />
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="userForm.gender">
        <el-radio v-for="dict in sys_user_gender" :key="dict.value" :value="dict.value">
          {{ dict.label }}
        </el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item class="profile-form__actions">
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { updateUserProfile } from '@/api/system/user';
import type { UserProfileForm } from '@/api/system/user/types';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';
import { useDict } from '@/utils/dict';
import { propTypes } from '@/utils/propTypes';

const { sys_user_gender } = toRefs<any>(useDict('sys_user_gender'));
const props = defineProps({
  user: propTypes.any.isRequired
});
const userForm = computed(() => props.user);
const userRef = ref<ElFormInstance>();
const rule: ElFormRules = {
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱地址不能为空', trigger: 'blur' },
    {
      type: 'email',
      message: '请输入正确的邮箱地址',
      trigger: ['blur', 'change']
    }
  ],
  phoneNumber: [
    {
      required: true,
      message: '手机号码不能为空',
      trigger: 'blur'
    },
    {
      pattern: /^1[3456789][0-9]\d{8}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur'
    }
  ]
};
const rules = ref<ElFormRules>(rule);

/** 提交按钮 */
const submit = () => {
  userRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      const profile: UserProfileForm = {
        nickName: props.user.nickName,
        phoneNumber: props.user.phoneNumber,
        email: props.user.email,
        gender: props.user.gender
      };
      await updateUserProfile(profile);
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

.profile-form :deep(.el-radio-group) {
  gap: 16px;
}

.profile-form :deep(.el-button) {
  border-radius: 10px;
}

.profile-form__actions :deep(.el-form-item__content) {
  display: flex;
  gap: 8px;
}
</style>
