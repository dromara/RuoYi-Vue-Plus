<template>
  <div class="p-2">
    <el-card shadow="never">
      <!-- mode用于直接后端发起流程 不同接口实现方式可查看具体后端代码 -->
      <!-- 默认前端发起 前端发起更多样性 比如可以选审批人 选抄送人 上传附件等等 后端发起需要用户自行编写代码传这些参数 -->
      <approvalButton
        @submitForm="submitForm"
        @approvalVerifyOpen="approvalVerifyOpen"
        @handleApprovalRecord="handleApprovalRecord"
        :buttonLoading="buttonLoading"
        :id="form.id"
        :business-id="routeParams.id"
        :instance-id="routeParams.instanceId"
        :status="form.status"
        :pageType="routeParams.type"
        :mode="false"
      />
    </el-card>
    <el-card shadow="never" style="height: 78vh; overflow-y: auto">
      <el-form
        ref="leaveFormRef"
        v-loading="loading"
        :disabled="routeParams.type === 'view'"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="流程定义" v-if="routeParams.type === 'add' || routeParams.type === 'update'">
          <el-select v-model="flowCode" :disabled="flowCodeLocked" placeholder="选择流程定义" style="width: 100%">
            <el-option v-for="item in flowCodeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width: 100%">
            <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间" required>
          <el-date-picker
            v-model="leaveTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            range-separator="To"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            @change="changeLeaveTime()"
          />
        </el-form-item>
        <el-form-item label="请假天数" prop="leaveDays">
          <el-input v-model="form.leaveDays" disabled type="number" placeholder="请输入请假天数" />
        </el-form-item>
        <el-form-item label="请假原因" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
    </el-card>
    <!-- 提交组件 -->
    <submitVerify ref="submitVerifyRef" :task-variables="taskVariables" @submit-callback="submitCallback" />
    <!-- 审批记录 -->
    <approvalRecord ref="approvalRecordRef" />
  </div>
</template>

<script setup name="Leave" lang="ts">
import { useRoute } from 'vue-router';
import { listDefinition } from '@/api/workflow/definition';
import { addLeave, getLeave, submitAndFlowStart, updateLeave } from '@/api/workflow/leave';
import { LeaveForm, LeaveQuery, LeaveVO } from '@/api/workflow/leave/types';
import { startWorkFlow } from '@/api/workflow/task';
import { StartProcessBo } from '@/api/workflow/workflowCommon/types';
import ApprovalButton from '@/components/Process/approvalButton.vue';
import ApprovalRecord from '@/components/Process/approvalRecord.vue';
import SubmitVerify from '@/components/Process/submitVerify.vue';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';
import router from '@/router';

const route = useRoute();

const buttonLoading = ref(false);
const loading = ref(true);
const leaveTime = ref<Array<string>>([]);
//路由参数
const routeParams = ref<Record<string, any>>({});
const options = [
  {
    value: '1',
    label: '事假'
  },
  {
    value: '2',
    label: '调休'
  },
  {
    value: '3',
    label: '病假'
  },
  {
    value: '4',
    label: '婚假'
  }
];
/** 可选流程定义：拉取真实已发布定义，避免写死早已不存在的流程编码 */
const flowCodeOptions = ref<Array<{ value: string; label: string }>>([]);
const loadFlowCodeOptions = async () => {
  const resp = await listDefinition({ pageNum: 1, pageSize: 200, category: undefined });
  flowCodeOptions.value = (resp.data?.rows ?? [])
    .filter(item => item.isPublish === 1)
    .map(item => ({ value: item.flowCode, label: `${item.flowName}（${item.flowCode}）` }));
  if (!flowCodeOptions.value.some(item => item.value === flowCode.value)) {
    flowCode.value = flowCodeOptions.value[0]?.value ?? '';
  }
};
// 下拉项来自已发布的流程定义
const flowCode = ref<string>('');
// 已有流程实例时沿用原流程，不允许在修改页切换
const flowCodeLocked = ref(false);

//提交组件
const submitVerifyRef = ref<InstanceType<typeof SubmitVerify>>();
//审批记录组件
const approvalRecordRef = ref<InstanceType<typeof ApprovalRecord>>();

const leaveFormRef = ref<ElFormInstance>();

const submitFormData = ref<StartProcessBo>({
  businessId: '',
  flowCode: '',
  variables: {},
  bizExt: {}
});
const taskVariables = ref<Record<string, any>>({});
const bizExt = ref<Record<string, any>>({});

const initFormData: LeaveForm = {
  id: undefined,
  leaveType: undefined,
  startDate: undefined,
  endDate: undefined,
  leaveDays: undefined,
  remark: undefined,
  status: undefined
};
const data = reactive<PageData<LeaveForm, LeaveQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    startLeaveDays: undefined,
    endLeaveDays: undefined
  },
  rules: {
    id: [{ required: true, message: '主键不能为空', trigger: 'blur' }],
    leaveType: [{ required: true, message: '请假类型不能为空', trigger: 'blur' }],
    leaveTime: [{ required: true, message: '请假时间不能为空', trigger: 'blur' }],
    leaveDays: [{ required: true, message: '请假天数不能为空', trigger: 'blur' }]
  }
});

const { form, rules } = toRefs(data);

/** 表单重置 */
const reset = () => {
  form.value = { ...initFormData };
  leaveTime.value = [];
  leaveFormRef.value?.resetFields();
};

const changeLeaveTime = () => {
  const startDate = new Date(leaveTime.value[0]).getTime();
  const endDate = new Date(leaveTime.value[1]).getTime();
  const diffInMilliseconds = endDate - startDate;
  form.value.leaveDays = Math.floor(diffInMilliseconds / (1000 * 60 * 60 * 24)) + 1;
};
/** 获取详情 */
const getInfo = () => {
  loading.value = true;
  buttonLoading.value = false;
  nextTick(async () => {
    const res = await getLeave(routeParams.value.id);
    Object.assign(form.value, res.data);
    leaveTime.value = [];
    leaveTime.value.push(form.value.startDate);
    leaveTime.value.push(form.value.endDate);
    if (routeParams.value.type === 'update') {
      await loadFlowCodeOptions();
      // 已有实例（撤销 / 退回 / 草稿已发起）沿用原流程定义；没有实例的草稿由用户自行选择
      if (res.data.flowCode) {
        flowCode.value = res.data.flowCode;
        flowCodeLocked.value = true;
      }
    }
    loading.value = false;
    buttonLoading.value = false;
  });
};

/** 提交按钮 */
const submitForm = (status: string, mode: boolean) => {
  if (leaveTime.value.length === 0) {
    modal.msgError('请假时间不能为空');
    return;
  }
  try {
    leaveFormRef.value?.validate(async (valid: boolean) => {
      form.value.startDate = leaveTime.value[0];
      form.value.endDate = leaveTime.value[1];
      if (valid) {
        buttonLoading.value = true;
        // 设置后端发起和不等于草稿状态 直接走流程发起
        if (mode && status != 'draft') {
          const res = await submitAndFlowStart(form.value).finally(() => (buttonLoading.value = false));
          form.value = res.data;
          buttonLoading.value = false;
          modal.msgSuccess('操作成功');
          tab.closePage(route);
          router.go(-1);
        } else {
          let res;
          if (form.value.id) {
            res = await updateLeave(form.value).finally(() => (buttonLoading.value = false));
          } else {
            res = await addLeave(form.value).finally(() => (buttonLoading.value = false));
          }
          form.value = res.data;
          if (status === 'draft') {
            buttonLoading.value = false;
            modal.msgSuccess('暂存成功');
            tab.closePage(route);
            router.go(-1);
          } else {
            await handleStartWorkFlow(res.data);
          }
        }
      }
    });
  } finally {
    buttonLoading.value = false;
  }
};

//提交申请
const handleStartWorkFlow = async (data: LeaveForm) => {
  if (!flowCode.value) {
    buttonLoading.value = false;
    modal.msgError('请选择流程定义');
    return;
  }
  try {
    submitFormData.value.flowCode = flowCode.value;
    submitFormData.value.businessId = data.id;
    //流程变量
    taskVariables.value = {
      // 网关示例的分支条件是 gt@@day|3 / lt@@day|3，变量名必须叫 day
      day: data.leaveDays
    };
    //流程实例业务扩展字段
    bizExt.value = {
      businessTitle: '请假申请',
      businessCode: data.applyCode
    };
    submitFormData.value.variables = taskVariables.value;
    submitFormData.value.bizExt = bizExt.value;
    const resp = await startWorkFlow(submitFormData.value);
    if (submitVerifyRef.value) {
      buttonLoading.value = false;
      submitVerifyRef.value.openDialog(resp.data.taskId);
    }
  } finally {
    buttonLoading.value = false;
  }
};
//审批记录
const handleApprovalRecord = () => {
  approvalRecordRef.value?.init({ businessId: routeParams.value.id || form.value.id, instanceId: routeParams.value.instanceId });
};
//提交回调
const submitCallback = async () => {
  await tab.closePage(route);
  router.go(-1);
};
//审批
const approvalVerifyOpen = async () => {
  submitVerifyRef.value.openDialog(routeParams.value.taskId);
};

onMounted(() => {
  nextTick(async () => {
    routeParams.value = route.query;
    reset();
    loading.value = false;
    if (routeParams.value.type === 'add') {
      loadFlowCodeOptions();
    }
    if (
      routeParams.value.type === 'update' ||
      routeParams.value.type === 'view' ||
      routeParams.value.type === 'approval'
    ) {
      getInfo();
    }
  });
});
</script>
