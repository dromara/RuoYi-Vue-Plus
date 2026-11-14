<template>
  <div style="display: flex; justify-content: space-between">
    <div>
      <el-button v-if="submitButtonShow" :loading="props.buttonLoading" type="info" @click="submitForm('draft', mode)">
        暂存
      </el-button>
      <el-button
        v-if="submitButtonShow"
        :loading="props.buttonLoading"
        type="primary"
        @click="submitForm('submit', mode)"
      >
        提 交
      </el-button>
      <el-button v-if="approvalButtonShow" :loading="props.buttonLoading" type="primary" @click="approvalVerifyOpen">
        审批
      </el-button>
      <el-button v-if="(props.id || props.businessId || props.instanceId) && props.status !== 'draft'" type="primary" @click="handleApprovalRecord">
        流程进度
      </el-button>
      <slot />
    </div>
    <div>
      <el-button style="float: right" @click="goBack()">返回</el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router';
import tab from '@/plugins/tab';
import router from '@/router';
import { propTypes } from '@/utils/propTypes';

const route = useRoute();
const props = defineProps({
  status: propTypes.string.def(''),
  pageType: propTypes.string.def(''),
  buttonLoading: propTypes.bool.def(false),
  id: propTypes.string.def('') || propTypes.number.def(),
  businessId: propTypes.string.def(''),
  instanceId: propTypes.string.def(''),
  mode: propTypes.bool.def(false)
});
const emits = defineEmits(['submitForm', 'approvalVerifyOpen', 'handleApprovalRecord']);
//暂存，提交
const submitForm = async (type, mode) => {
  emits('submitForm', type, mode);
};
//审批
const approvalVerifyOpen = async () => {
  emits('approvalVerifyOpen');
};
//审批记录
const handleApprovalRecord = () => {
  emits('handleApprovalRecord');
};

//校验提交按钮是否显示
const submitButtonShow = computed(() => {
  return (
    props.pageType === 'add' ||
    (props.pageType === 'update' &&
      props.status &&
      (props.status === 'draft' || props.status === 'cancel' || props.status === 'back'))
  );
});

//校验审批按钮是否显示
const approvalButtonShow = computed(() => {
  return props.pageType === 'approval' && props.status && props.status === 'waiting';
});

//返回
const goBack = () => {
  tab.closePage(route);
  router.go(-1);
};
</script>
