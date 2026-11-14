<template>
  <div class="p-2 app-container workflow-timer-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div><h3>筛选条件</h3></div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
          <el-form-item label="流程编码" prop="flowCode">
            <el-input
              v-model="queryParams.flowCode"
              placeholder="请输入流程编码"
              clearable
              style="width: 180px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="节点编码" prop="nodeCode">
            <el-input
              v-model="queryParams.nodeCode"
              placeholder="请输入事件节点编码"
              clearable
              style="width: 180px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="业务ID" prop="businessId">
            <el-input
              v-model="queryParams.businessId"
              placeholder="请输入业务主键"
              clearable
              style="width: 180px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="事件类型">
            <el-select
              v-model="selectedEventTypes"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="默认只查定时类"
              style="width: 220px"
            >
              <el-option
                v-for="item in EVENT_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="selectedStatuses"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="不限"
              style="width: 200px"
            >
              <el-option
                v-for="item in STATUS_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="下次触发">
            <el-date-picker
              v-model="nextTriggerRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width: 340px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>定时任务</h3>
            <el-tag v-if="summaryReady" type="info" class="summary-tag">
              待触发 {{ summary.waiting }} · 重试中 {{ summary.retry }} · 执行中 {{ summary.running }} · 已取消
              {{ summary.cancelled }} · 已死 {{ summary.dead }}
            </el-tag>
          </div>
          <div class="toolbar-actions">
            <el-button plain icon="Refresh" @click="getList">刷新</el-button>
            <right-toolbar v-model:showSearch="showSearch" :search="false" @queryTable="getList"></right-toolbar>
          </div>
        </div>
      </template>

      <el-alert
        v-if="timerDisabled"
        type="warning"
        :closable="false"
        class="mb-2"
        title="定时引擎扫描未开启（warm-flow.timer-enabled=false），等待记录不会被自动触发，可在此手动补发。"
      />

      <el-table v-loading="loading" border class="data-table" :data="waitList">
        <el-table-column label="流程名称" align="center" prop="flowName" min-width="140" show-overflow-tooltip />
        <el-table-column label="事件节点" align="center" prop="nodeCode" min-width="110" show-overflow-tooltip />
        <el-table-column label="事件类型" align="center" prop="eventTypeName" width="100" />
        <el-table-column label="触发策略" align="center" prop="triggerType" width="110">
          <template #default="scope">
            {{ TRIGGER_TYPE_NAMES[scope.row.triggerType || ''] || scope.row.triggerType || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="间隔/时点" align="center" prop="triggerInterval" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            {{ formatTriggerInterval(scope.row.triggerType, scope.row.triggerInterval) }}
          </template>
        </el-table-column>
        <el-table-column label="下次触发时间" align="center" prop="nextTriggerAt" width="160" />
        <el-table-column label="已触发" align="center" width="90">
          <template #default="scope">
            {{ scope.row.repeatCount ?? 0 }}{{ scope.row.maxRepeatCount ? ' / ' + scope.row.maxRepeatCount : '' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="90">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">{{ scope.row.statusName || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联实例" align="center" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.businessId || (scope.row.instanceId ? scope.row.instanceId : '定义级') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="250" fixed="right">
          <template #default="scope">
            <div class="timer-action-group">
              <el-tooltip
                v-if="scope.row.status === 'DEAD'"
                :disabled="!isTimerStart(scope.row)"
                content="定时启动任务请通过重新发布或挂起后激活恢复"
              >
                <el-button
                  v-hasPermi="['workflow:eventWait:trigger']"
                  link
                  type="warning"
                  icon="Refresh"
                  :disabled="isTimerStart(scope.row) || restoringId !== undefined"
                  :loading="restoringId === scope.row.id"
                  @click="handleRevive(scope.row)"
                >
                  手动恢复
                </el-button>
              </el-tooltip>
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
              <el-button
                v-if="isTimerStart(scope.row)"
                link
                type="success"
                icon="VideoPlay"
                v-hasPermi="['workflow:eventWait:trigger']"
                @click="handleTrigger(scope.row)"
              >
                立即触发
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="定时任务详情" size="480px">
      <el-descriptions :column="1" border class="timer-detail-descriptions">
        <el-descriptions-item label="主键">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="流程名称">{{ detail.flowName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件节点">{{ detail.nodeCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ detail.eventTypeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件名称">{{ detail.eventName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="触发策略">{{ TRIGGER_TYPE_NAMES[detail.triggerType || ''] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="间隔/时点">{{ formatTriggerInterval(detail.triggerType, detail.triggerInterval) }}</el-descriptions-item>
        <el-descriptions-item label="时区">{{ detail.timezone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下次触发">{{ detail.nextTriggerAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusName || detail.status }}</el-descriptions-item>
        <el-descriptions-item label="已触发次数">{{ detail.repeatCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ detail.retryCount ?? 0 }} / {{ detail.maxRetryCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="下次重试">{{ detail.retryAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联实例">{{ detail.businessId || (detail.instanceId ? detail.instanceId : '定义级') }}</el-descriptions-item>
        <el-descriptions-item label="载荷">{{ detail.payload || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近错误">{{ detail.lastError || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 手动补发对话框 -->
    <el-dialog v-model="triggerVisible" title="立即触发一次" width="540px" :close-on-click-modal="false">
      <el-form label-width="96px">
        <el-form-item label="任务">
          <span>{{ triggerRow?.flowName || '-' }} / {{ triggerRow?.nodeCode || '-' }}</span>
        </el-form-item>
        <el-form-item label="下次触发">
          <span>{{ triggerRow?.nextTriggerAt || '-' }}</span>
        </el-form-item>
        <el-form-item label="业务ID">
          <el-input v-model="triggerForm.businessId" clearable placeholder="留空自动生成（long 型雪花ID）" />
        </el-form-item>
      </el-form>
      <div class="dialog-tip" style="font-size: 12px; color: var(--el-text-color-secondary)">
        只额外发起一次实例，操作人记为当前登录用户。定时任务本身不会变动，<b>下次触发时间保持不变</b>，
        例如每天 9 点的任务提前补发后，当天 9 点仍会照常触发。
      </div>
      <template #footer>
        <el-button @click="triggerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTrigger">确认触发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Timer" lang="ts">
import { listEventWait, getEventWait, summaryEventWait, triggerTimerStart, reviveEventWait } from '@/api/workflow/timer';
import type { EventWaitQuery, EventWaitSummary, EventWaitVO } from '@/api/workflow/timer/types';
import { useLoading } from '@/hooks/async/useLoading';
import { useSearchToggle } from '@/hooks/form/useSearchToggle';
import modal from '@/plugins/modal';

const EVENT_TYPE_OPTIONS = [
  { label: '定时启动', value: 'TIMER_START' },
  { label: '定时捕获', value: 'TIMER_CATCH' },
  { label: '定时边界', value: 'TIMER_BOUNDARY' },
  { label: '消息启动', value: 'MESSAGE_START' },
  { label: '消息捕获', value: 'MESSAGE_CATCH' },
  { label: '消息边界', value: 'MESSAGE_BOUNDARY' },
  { label: '错误捕获', value: 'ERROR_CATCH' },
  { label: '错误边界', value: 'ERROR_BOUNDARY' }
];

const STATUS_OPTIONS = [
  { label: '待触发', value: 'WAITING' },
  { label: '重试中', value: 'RETRY' },
  { label: '执行中', value: 'RUNNING' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已死', value: 'DEAD' }
];

const TRIGGER_TYPE_NAMES: Record<string, string> = {
  AFTER: '一次性延迟',
  FIXED: '固定间隔',
  POINT_IN_TIME: '每日时点'
};

/** ISO-8601 时长转中文：PT30S → "30 秒"，PT1H30M → "1 小时 30 分钟"，解析失败返回原文 */
function formatIsoDuration(value: string): string {
  const m = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/.exec(value.trim());
  if (!m) return value;
  const parts: string[] = [];
  if (m[1]) parts.push(`${m[1]} 天`);
  if (m[2]) parts.push(`${m[2]} 小时`);
  if (m[3]) parts.push(`${m[3]} 分钟`);
  if (m[4]) parts.push(`${m[4]} 秒`);
  return parts.length ? parts.join(' ') : value;
}

/** 间隔/时点列展示：按触发策略把原始值转成可读描述，解析失败按原文展示 */
function formatTriggerInterval(triggerType: string | undefined, triggerInterval: string | undefined): string {
  const raw = (triggerInterval || '').trim();
  if (!raw) return '-';
  try {
    if (triggerType === 'POINT_IN_TIME') {
      const arr = JSON.parse(raw);
      const times = Array.isArray(arr) ? arr : [raw];
      return times.length ? `每日 ${times.map((t: string) => String(t)).join('、')}` : raw;
    }
    if (triggerType === 'AFTER') {
      let value = raw;
      try {
        // 兼容 JSON 数组存储；纯时长字符串（非合法 JSON）按原文处理
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length) value = String(parsed[0]);
      } catch {
        // 纯时长字符串
      }
      const dur = formatIsoDuration(value);
      return dur === value ? raw : `${dur}后`;
    }
    if (triggerType === 'FIXED') {
      const dur = formatIsoDuration(raw);
      return dur === raw ? raw : `每 ${dur}`;
    }
  } catch {
    // 非法 JSON 或未知格式，按原文展示
  }
  return raw;
}

const { showSearch } = useSearchToggle();
const { loading, withLoading } = useLoading(true);
const queryFormRef = ref<ElFormInstance>();

const waitList = ref<EventWaitVO[]>([]);
const total = ref(0);
const summaryReady = ref(false);
const timerDisabled = ref(false);
const summary = reactive<EventWaitSummary>({ waiting: 0, retry: 0, running: 0, dead: 0, cancelled: 0 });

const selectedEventTypes = ref<string[]>([]);
const selectedStatuses = ref<string[]>([]);
const nextTriggerRange = ref<[string, string] | undefined>();

const queryParams = ref<EventWaitQuery>({
  pageNum: 1,
  pageSize: 10,
  flowCode: undefined,
  nodeCode: undefined,
  businessId: undefined,
  timerOnly: undefined
});

const detailVisible = ref(false);
const detail = ref<Partial<EventWaitVO>>({});

const triggerVisible = ref(false);
const triggerRow = ref<EventWaitVO | null>(null);
const triggerForm = reactive({ businessId: '' });
const submitting = ref(false);
const restoringId = ref<string | number>();

const buildQuery = (): EventWaitQuery => {
  return {
    ...queryParams.value,
    statuses: selectedStatuses.value.length ? selectedStatuses.value.join(',') : undefined,
    eventTypes: selectedEventTypes.value.length ? selectedEventTypes.value.join(',') : undefined,
    nextTriggerAtFrom: nextTriggerRange.value?.[0],
    nextTriggerAtTo: nextTriggerRange.value?.[1]
  };
};

const getList = async () => {
  await withLoading(async () => {
    const query = buildQuery();
    const res = await listEventWait(query);
    waitList.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
    timerDisabled.value = waitList.value.length > 0 && waitList.value[0].timerEnabled === false;
    const sumRes = await summaryEventWait(query);
    Object.assign(summary, sumRes.data ?? {});
    summaryReady.value = true;
  });
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryParams.value = { pageNum: 1, pageSize: 10 };
  selectedEventTypes.value = [];
  selectedStatuses.value = [];
  nextTriggerRange.value = undefined;
  queryFormRef.value?.resetFields();
  handleQuery();
};

const statusTagType = (status?: string) => {
  switch (status) {
    case 'WAITING':
      return 'primary';
    case 'RETRY':
      return 'warning';
    case 'RUNNING':
      return 'success';
    case 'DEAD':
      return 'danger';
    case 'CANCELLED':
      return 'info';
    default:
      return 'info';
  }
};

const isTimerStart = (row: EventWaitVO | Record<string, any>) => (row as EventWaitVO).eventType === 'TIMER_START';

const handleDetail = async (row: EventWaitVO | Record<string, any>) => {
  const res = await getEventWait((row as EventWaitVO).id);
  detail.value = res.data ?? row;
  detailVisible.value = true;
};

const handleTrigger = (row: EventWaitVO | Record<string, any>) => {
  triggerRow.value = row as EventWaitVO;
  triggerForm.businessId = '';
  triggerVisible.value = true;
};

const handleRevive = async (row: EventWaitVO | Record<string, any>) => {
  if (restoringId.value !== undefined || row.status !== 'DEAD' || isTimerStart(row)) return;
  restoringId.value = row.id;
  try {
    const confirmed = await modal
      .confirm('确认恢复该事件等待记录？恢复后重试次数归零，定时事件将从当前时间重新排期。')
      .then(() => true, () => false);
    if (!confirmed) return;
    await reviveEventWait(row.id);
    modal.msgSuccess('已恢复');
    await getList();
    if (detailVisible.value && detail.value.id === row.id) {
      await handleDetail(row);
    }
  } finally {
    restoringId.value = undefined;
  }
};

const submitTrigger = async () => {
  const row = triggerRow.value;
  if (!row) return;
  submitting.value = true;
  try {
    const res = await triggerTimerStart({
      waitId: row.id,
      businessId: triggerForm.businessId.trim() || undefined
    });
    modal.msgSuccess(`已发起一次实例 ${res.data}`);
    triggerVisible.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  getList();
});
</script>

<style scoped lang="scss">
.workflow-timer-page {
  .summary-tag {
    margin-left: 12px;
  }
}

.timer-detail-descriptions {
  :deep(.el-descriptions__label) {
    min-width: 112px;
    white-space: nowrap;
  }

  :deep(.el-descriptions__content) {
    overflow-wrap: anywhere;
  }
}

/* 操作列链接按钮：全局 .data-table .el-button.is-link 是图标尺寸（28x28），
   这里按钮带文字，需还原成自适应宽度并留出正文内边距 */
.timer-action-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 6px;
  padding: 2px 0;

  :deep(.el-button.is-link) {
    width: auto !important;
    min-width: 0 !important;
    height: 28px !important;
    padding: 0 8px !important;
    border-radius: 8px !important;
  }

  :deep(.el-button.is-link + .el-button.is-link) {
    margin-left: 0 !important;
  }

  :deep(.el-button .el-icon) {
    margin-right: 3px;
  }
}
</style>
