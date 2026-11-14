<template>
  <el-dialog
    v-model="visible"
    :title="props.title"
    width="50%"
    draggable
    :before-close="cancel"
    center
    :close-on-click-modal="false"
  >
    <el-form v-loading="loading" ref="ruleFormRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="消息提醒" prop="messageType">
        <el-checkbox-group v-model="form.messageType">
          <el-checkbox value="1" name="type" disabled>站内信</el-checkbox>
          <el-checkbox value="2" name="type">邮件</el-checkbox>
          <el-checkbox value="3" name="type">短信</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="消息内容" prop="message">
        <el-input v-model="form.message" type="textarea" resize="none" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer" style="float: right; padding-bottom: 20px">
        <el-button :disabled="buttonDisabled" type="primary" @click="submit(ruleFormRef)">确认</el-button>
        <el-button :disabled="buttonDisabled" @click="cancel">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ElForm, FormInstance } from 'element-plus';
import { ref } from 'vue';

const emits = defineEmits(['submitCallback', 'cancelCallback']);
const props = defineProps({
  title: {
    type: String,
    default: '提示'
  }
});
const ruleFormRef = ref<FormInstance>();
//遮罩层
const loading = ref(true);
const visible = ref(false);
const buttonDisabled = ref(true);
const form = ref<Record<string, any>>({
  message: undefined,
  messageType: ['1']
});
const rules = reactive<Record<string, any>>({
  messageType: [
    {
      required: true,
      message: '请选择消息提醒',
      trigger: 'change'
    }
  ],
  message: [
    {
      required: true,
      message: '请输入消息内容',
      trigger: 'blur'
    }
  ]
});
//确认
//打开弹窗
const open = async () => {
  reset();
  visible.value = true;
  loading.value = false;
  buttonDisabled.value = false;
};
//关闭弹窗
const close = async () => {
  reset();
  visible.value = false;
};
const submit = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  await formEl.validate((valid, fields) => {
    if (valid) {
      emits('submitCallback', form.value);
    }
  });
};
//取消
const cancel = async () => {
  visible.value = false;
  buttonDisabled.value = false;
  emits('cancelCallback');
};
//重置
const reset = async () => {
  form.value.taskIdList = [];
  form.value.message = '';
  form.value.messageType = ['1'];
};
/**
 * 对外暴露子组件方法
 */
defineExpose({
  open,
  close
});
</script>
