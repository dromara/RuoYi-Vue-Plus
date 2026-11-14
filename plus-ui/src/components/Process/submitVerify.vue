<template>
  <el-dialog
    v-model="dialog.visible"
    :title="dialog.title"
    width="50%"
    draggable
    :before-close="cancel"
    center
    :close-on-click-modal="false"
  >
    <el-form v-loading="loading" :model="form" label-width="120px">
      <el-form-item label="消息提醒">
        <el-checkbox-group v-model="form.messageType">
          <el-checkbox value="1" name="type" disabled>站内信</el-checkbox>
          <el-checkbox value="2" name="type">邮件</el-checkbox>
          <el-checkbox value="3" name="type">短信</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="附件" v-if="buttonObj.file">
        <fileUpload
          v-model="form.fileId"
          :file-type="['png', 'jpg', 'jpeg', 'doc', 'docx', 'xlsx', 'xls', 'ppt', 'txt', 'pdf']"
          :file-size="20"
        />
      </el-form-item>
      <el-form-item label="抄送" v-if="buttonObj.copy">
        <el-button type="primary" icon="Plus" circle @click="openUserSelectCopy" />
        <el-tag
          v-for="user in selectCopyUserList"
          :key="user.userId"
          closable
          style="margin: 2px"
          @close="handleCopyCloseTag(user)"
        >
          {{ user.nickName }}
        </el-tag>
      </el-form-item>
      <el-form-item
        v-if="buttonObj.pop && nestNodeList && nestNodeList.length > 0"
        label="下一步审批人"
        prop="assigneeMap"
      >
        <div v-for="(item, index) in nestNodeList" :key="index" style="margin-bottom: 5px; width: 500px">
          <span>【{{ item.nodeName }}】：</span>
          <el-input v-if="false" v-model="form.assigneeMap[item.nodeCode]" />
          <el-input placeholder="请选择审批人" readonly v-model="nickName[item.nodeCode]">
            <template v-slot:append>
              <el-button @click="choosePeople(item)" icon="search">选择</el-button>
            </template>
          </el-input>
        </div>
      </el-form-item>
      <el-form-item v-if="task.flowStatus === 'waiting'" label="审批意见">
        <el-input v-model="form.message" type="textarea" resize="none" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button :disabled="buttonDisabled" type="primary" @click="handleCompleteTask">提交</el-button>
        <el-button
          v-if="task.flowStatus === 'waiting' && buttonObj.trust"
          :disabled="buttonDisabled"
          type="primary"
          @click="openDelegateTask"
        >
          委托
        </el-button>
        <el-button
          v-if="task.flowStatus === 'waiting' && buttonObj.transfer"
          :disabled="buttonDisabled"
          type="primary"
          @click="openTransferTask"
        >
          转办
        </el-button>
        <el-button
          v-if="task.flowStatus === 'waiting' && Number(task.nodeRatio) > 0 && buttonObj.addSign"
          :disabled="buttonDisabled"
          type="primary"
          @click="openMultiInstanceUser"
        >
          加签
        </el-button>
        <el-button
          v-if="task.flowStatus === 'waiting' && Number(task.nodeRatio) > 0 && buttonObj.subSign"
          :disabled="buttonDisabled"
          type="primary"
          @click="handleTaskUser"
        >
          减签
        </el-button>
        <el-button
          v-if="task.flowStatus === 'waiting' && buttonObj.termination"
          :disabled="buttonDisabled"
          type="danger"
          @click="handleTerminationTask"
        >
          终止
        </el-button>
        <el-button
          v-if="task.flowStatus === 'waiting' && buttonObj.back"
          :disabled="buttonDisabled"
          type="danger"
          @click="handleBackProcessOpen"
        >
          退回
        </el-button>
        <el-button :disabled="buttonDisabled" @click="cancel">取消</el-button>
      </span>
    </template>
    <!-- 抄送 -->
    <UserSelect
      ref="userSelectCopyRef"
      :multiple="true"
      :data="selectCopyUserIds"
      @confirm-call-back="userSelectCopyCallBack"
    ></UserSelect>
    <!-- 转办 -->
    <UserSelect ref="transferTaskRef" :multiple="false" @confirm-call-back="handleTransferTask"></UserSelect>
    <!-- 委托 -->
    <UserSelect ref="delegateTaskRef" :multiple="false" @confirm-call-back="handleDelegateTask"></UserSelect>
    <!-- 加签组件 -->
    <UserSelect ref="multiInstanceUserRef" :multiple="true" @confirm-call-back="addMultiInstanceUser"></UserSelect>
    <!-- 弹窗选人 -->
    <UserSelect
      ref="porUserRef"
      :data="form.assigneeMap[nodeCode]"
      :multiple="true"
      :userIds="popUserIds"
      @confirm-call-back="handlePopUser"
    ></UserSelect>

    <!-- 驳回开始 -->
    <el-dialog v-model="backVisible" draggable title="驳回" width="40%" :close-on-click-modal="false">
      <el-form v-if="task.flowStatus === 'waiting'" v-loading="backLoading" :model="backForm" label-width="120px">
        <el-form-item label="驳回节点">
          <el-select v-model="backForm.nodeCode" clearable placeholder="请选择" style="width: 300px">
            <el-option
              v-for="item in taskNodeList"
              :key="item.nodeCode"
              :label="item.nodeName"
              :value="item.nodeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="消息提醒">
          <el-checkbox-group v-model="backForm.messageType">
            <el-checkbox value="1" name="type" disabled>站内信</el-checkbox>
            <el-checkbox value="2" name="type">邮件</el-checkbox>
            <el-checkbox value="3" name="type">短信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="task.flowStatus === 'waiting'" label="附件">
          <fileUpload
            v-model="backForm.fileId"
            :file-type="['png', 'jpg', 'jpeg', 'doc', 'docx', 'xlsx', 'xls', 'ppt', 'txt', 'pdf']"
            :file-size="20"
          />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="backForm.message" type="textarea" resize="none" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer" style="float: right; padding-bottom: 20px">
          <el-button :disabled="backButtonDisabled" type="primary" @click="handleBackProcess">确认</el-button>
          <el-button :disabled="backButtonDisabled" @click="backVisible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 驳回结束 -->
    <el-dialog
      v-model="deleteSignatureVisible"
      draggable
      title="减签人员"
      width="700px"
      height="400px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div>
        <el-table :data="deleteUserList" border>
          <el-table-column prop="nodeName" label="任务名称" />
          <el-table-column prop="nickName" label="办理人" />
          <el-table-column label="操作" align="center" width="160">
            <template #default="scope">
              <el-button type="danger" size="small" icon="Delete" @click="deleteMultiInstanceUser(scope.row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElForm } from 'element-plus';
import { ref } from 'vue';
import {
  completeTask,
  backProcess,
  getTask,
  taskOperation,
  terminationTask,
  getBackTaskNode,
  currentTaskAllUser,
  getNextNodeList
} from '@/api/workflow/task';
import { FlowCopyVo, FlowTaskVO, TaskOperationBo } from '@/api/workflow/task/types';
import UserSelect from '@/components/UserSelect/index.vue';
import modal from '@/plugins/modal';

const userSelectCopyRef = ref<InstanceType<typeof UserSelect>>();
const transferTaskRef = ref<InstanceType<typeof UserSelect>>();
const delegateTaskRef = ref<InstanceType<typeof UserSelect>>();
const multiInstanceUserRef = ref<InstanceType<typeof UserSelect>>();
const porUserRef = ref<InstanceType<typeof UserSelect>>();

const props = defineProps({
  taskVariables: {
    type: Object as () => Record<string, any>,
    default: () => ({})
  }
});
//遮罩层
const loading = ref(true);
//按钮
const buttonDisabled = ref(true);
//任务id
const taskId = ref<string>('');
//抄送人
const selectCopyUserList = ref<FlowCopyVo[]>([]);
//抄送人id
const selectCopyUserIds = ref<string>(undefined);
//自定义节点变量
const varNodeList = ref<Map<string, string>>(undefined);
//可减签的人员
const deleteUserList = ref<any>([]);
//弹窗可选择的人员id
const popUserIds = ref<any>([]);
//驳回是否显示
const backVisible = ref(false);
const backLoading = ref(true);
const backButtonDisabled = ref(true);
// 可驳回得任务节点
const taskNodeList = ref([]);
const nickName = ref({});
//节点编码
const nodeCode = ref<string>('');
const buttonObj = ref<any>({
  pop: false,
  trust: false,
  transfer: false,
  addSign: false,
  subSign: false,
  termination: false,
  back: false
});
//下一节点列表
const nestNodeList = ref([]);
//任务
const task = ref<FlowTaskVO>({
  id: undefined,
  createTime: undefined,
  updateTime: undefined,
  tenantId: undefined,
  definitionId: undefined,
  instanceId: undefined,
  flowName: undefined,
  businessId: undefined,
  nodeCode: undefined,
  nodeName: undefined,
  flowCode: undefined,
  flowStatus: undefined,
  formCustom: undefined,
  formPath: undefined,
  nodeType: undefined,
  nodeRatio: undefined,
  applyNode: false,
  buttonList: [],
  copyList: [],
  varList: undefined,
  businessCode: undefined,
  businessTitle: undefined
});
const dialog = reactive<DialogOption>({
  visible: false,
  title: '提示'
});
//减签弹窗
const deleteSignatureVisible = ref(false);
const form = ref<Record<string, any>>({
  taskId: undefined,
  message: undefined,
  assigneeMap: {},
  variables: {},
  messageType: ['1'],
  flowCopyList: []
});
const backForm = ref<Record<string, any>>({
  taskId: undefined,
  nodeCode: undefined,
  message: undefined,
  variables: {},
  messageType: ['1']
});

//打开弹窗
const openDialog = async (id?: string) => {
  selectCopyUserIds.value = undefined;
  selectCopyUserList.value = [];
  form.value.fileId = undefined;
  taskId.value = id;
  form.value.message = undefined;
  dialog.visible = true;
  loading.value = true;
  buttonDisabled.value = true;
  const response = await getTask(taskId.value);
  task.value = response.data;
  buttonObj.value = {};
  task.value.buttonList?.forEach(e => {
    buttonObj.value[e.code] = e.show;
  });
  selectCopyUserList.value = task.value.copyList;
  selectCopyUserIds.value = task.value.copyList.map(e => e.userId).join(',');
  varNodeList.value = task.value.varList;
  buttonDisabled.value = false;
  try {
    const data = {
      taskId: taskId.value,
      variables: props.taskVariables
    };
    const nextData = await getNextNodeList(data);
    nestNodeList.value = nextData.data;
  } finally {
    loading.value = false;
  }
};

const emits = defineEmits(['submitCallback', 'cancelCallback']);

/** 办理流程 */
const handleCompleteTask = async () => {
  form.value.taskId = taskId.value;
  form.value.variables = props.taskVariables;
  let verify = false;
  if (buttonObj.value.pop && nestNodeList.value && nestNodeList.value.length > 0) {
    nestNodeList.value.forEach(e => {
      if (
        Object.keys(form.value.assigneeMap).length === 0 ||
        form.value.assigneeMap[e.nodeCode] === '' ||
        form.value.assigneeMap[e.nodeCode] === null ||
        form.value.assigneeMap[e.nodeCode] === undefined
      ) {
        verify = true;
      }
    });
    if (verify) {
      modal.msgWarning('请选择审批人！');
      return false;
    }
  } else {
    form.value.assigneeMap = {};
  }
  if (selectCopyUserList.value && selectCopyUserList.value.length > 0) {
    const flowCopyList = [];
    selectCopyUserList.value.forEach(e => {
      const copyUser = {
        userId: e.userId,
        nickName: e.nickName
      };
      flowCopyList.push(copyUser);
    });
    form.value.flowCopyList = flowCopyList;
  }
  await modal.confirm('是否确认提交？');
  loading.value = true;
  buttonDisabled.value = true;
  try {
    await completeTask(form.value);
    dialog.visible = false;
    emits('submitCallback');
    modal.msgSuccess('操作成功');
  } finally {
    loading.value = false;
    buttonDisabled.value = false;
  }
};

/** 驳回弹窗打开 */
const handleBackProcessOpen = async () => {
  backForm.value = {};
  backForm.value.messageType = ['1'];
  backVisible.value = true;
  backLoading.value = true;
  backButtonDisabled.value = true;
  const data = await getBackTaskNode(task.value.id, task.value.nodeCode);
  taskNodeList.value = data.data;
  backLoading.value = false;
  backButtonDisabled.value = false;
  backForm.value.nodeCode = taskNodeList.value[0].nodeCode;
};
/** 驳回流程 */
const handleBackProcess = async () => {
  backForm.value.taskId = taskId.value;
  await modal.confirm('是否确认驳回到申请人？');
  loading.value = true;
  backLoading.value = true;
  backButtonDisabled.value = true;
  await backProcess(backForm.value).finally(() => {
    loading.value = false;
    buttonDisabled.value = false;
  });
  dialog.visible = false;
  backLoading.value = false;
  backButtonDisabled.value = false;
  emits('submitCallback');
  modal.msgSuccess('操作成功');
};
//取消
const cancel = async () => {
  dialog.visible = false;
  buttonDisabled.value = false;
  nickName.value = {};
  form.value.assigneeMap = {};
  emits('cancelCallback');
};
//打开抄送人员
const openUserSelectCopy = () => {
  userSelectCopyRef.value.open();
};
//确认抄送人员
const userSelectCopyCallBack = (data: FlowCopyVo[]) => {
  if (data && data.length > 0) {
    selectCopyUserList.value = data;
    selectCopyUserIds.value = selectCopyUserList.value.map(item => item.userId).join(',');
  }
};
//删除抄送人员
const handleCopyCloseTag = (user: FlowCopyVo) => {
  const userId = user.userId;
  // 使用split删除用户
  const index = selectCopyUserList.value.findIndex(item => item.userId === userId);
  selectCopyUserList.value.splice(index, 1);
  selectCopyUserIds.value = selectCopyUserList.value.map(item => item.userId).join(',');
};
//加签
const openMultiInstanceUser = async () => {
  multiInstanceUserRef.value.open();
};
//加签
const addMultiInstanceUser = async data => {
  if (data && data.length > 0) {
    const taskOperationBo = reactive<TaskOperationBo>({
      userIds: data.map(e => e.userId),
      taskId: taskId.value,
      message: form.value.message,
      messageType: ['1']
    });
    await modal.confirm('是否确认提交？');
    loading.value = true;
    buttonDisabled.value = true;
    await taskOperation(taskOperationBo, 'addSignature').finally(() => {
      loading.value = false;
      buttonDisabled.value = false;
    });
    dialog.visible = false;
    emits('submitCallback');
    modal.msgSuccess('操作成功');
  } else {
    modal.msgWarning('请选择用户！');
  }
};
//减签
const deleteMultiInstanceUser = async row => {
  await modal.confirm('是否确认提交？');
  loading.value = true;
  buttonDisabled.value = true;
  const taskOperationBo = reactive<TaskOperationBo>({
    userIds: [row.userId],
    taskId: taskId.value,
    message: form.value.message,
    messageType: ['1']
  });
  await taskOperation(taskOperationBo, 'reductionSignature').finally(() => {
    loading.value = false;
    buttonDisabled.value = false;
  });
  dialog.visible = false;
  emits('submitCallback');
  modal.msgSuccess('操作成功');
};
//打开转办
const openTransferTask = () => {
  transferTaskRef.value.open();
};
//转办
const handleTransferTask = async data => {
  if (data && data.length > 0) {
    const taskOperationBo = reactive<TaskOperationBo>({
      userId: data[0].userId,
      taskId: taskId.value,
      message: form.value.message,
      messageType: ['1']
    });
    await modal.confirm('是否确认提交？');
    loading.value = true;
    buttonDisabled.value = true;
    await taskOperation(taskOperationBo, 'transferTask').finally(() => {
      loading.value = false;
      buttonDisabled.value = false;
    });
    dialog.visible = false;
    emits('submitCallback');
    modal.msgSuccess('操作成功');
  } else {
    modal.msgWarning('请选择用户！');
  }
};

//打开委托
const openDelegateTask = () => {
  delegateTaskRef.value.open();
};
//委托
const handleDelegateTask = async data => {
  if (data && data.length > 0) {
    const taskOperationBo = reactive<TaskOperationBo>({
      userId: data[0].userId,
      taskId: taskId.value,
      message: form.value.message,
      messageType: ['1']
    });
    await modal.confirm('是否确认提交？');
    loading.value = true;
    buttonDisabled.value = true;
    await taskOperation(taskOperationBo, 'delegateTask').finally(() => {
      loading.value = false;
      buttonDisabled.value = false;
    });
    dialog.visible = false;
    emits('submitCallback');
    modal.msgSuccess('操作成功');
  } else {
    modal.msgWarning('请选择用户！');
  }
};
//终止任务
const handleTerminationTask = async () => {
  const params = {
    taskId: taskId.value,
    comment: form.value.message
  };
  await modal.confirm('是否确认终止？');
  loading.value = true;
  buttonDisabled.value = true;
  await terminationTask(params).finally(() => {
    loading.value = false;
    buttonDisabled.value = false;
  });
  dialog.visible = false;
  emits('submitCallback');
  modal.msgSuccess('操作成功');
};
const handleTaskUser = async () => {
  const data = await currentTaskAllUser(taskId.value);
  deleteUserList.value = data.data;
  if (deleteUserList.value && deleteUserList.value.length > 0) {
    deleteUserList.value.forEach(e => {
      e.nodeName = task.value.nodeName;
    });
  }
  deleteSignatureVisible.value = true;
};
// 选择人员
const choosePeople = async data => {
  if (!data.permissionFlag) {
    modal.msgError('没有可选择的人员，请联系管理员！');
  }
  popUserIds.value = data.permissionFlag;
  nodeCode.value = data.nodeCode;
  porUserRef.value.open();
};
//确认选择
const handlePopUser = async userList => {
  const userIds = userList.map(item => {
    return item.userId;
  });
  const nickNames = userList.map(item => {
    return item.nickName;
  });
  form.value.assigneeMap[nodeCode.value] = userIds.join(',');
  nickName.value[nodeCode.value] = nickNames.join(',');
};

/**
 * 对外暴露子组件方法
 */
defineExpose({
  openDialog
});
</script>
