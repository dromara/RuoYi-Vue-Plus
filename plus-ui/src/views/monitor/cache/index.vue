<template>
  <div class="p-2 app-container monitor-cache-page">
    <el-row :gutter="12" class="cache-grid">
      <el-col :span="24">
        <el-card shadow="hover" class="table-panel">
          <template #header>
            <div class="toolbar-shell">
              <div class="table-heading">
                <h3>缓存概览</h3>
                <p>Redis 运行状态、资源消耗与实时统计。</p>
              </div>
            </div>
          </template>

          <div class="el-table el-table--enable-row-hover el-table--medium cache-table">
            <table style="width: 100%">
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">Redis版本</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">{{ cache.info.redis_version }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">运行模式</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">
                      {{ cache.info.redis_mode === 'standalone' ? '单机' : '集群' }}
                    </div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">端口</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">{{ cache.info.tcp_port }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">客户端数</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">{{ cache.info.connected_clients }}</div>
                  </td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">运行时间(天)</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">{{ cache.info.uptime_in_days }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">使用内存</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">{{ cache.info.used_memory_human }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">使用CPU</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">
                      {{ parseFloat(cache.info.used_cpu_user_children).toFixed(2) }}
                    </div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">内存配置</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">{{ cache.info.maxmemory_human }}</div>
                  </td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">AOF是否开启</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">
                      {{ cache.info.aof_enabled === '0' ? '否' : '是' }}
                    </div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">RDB是否成功</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">
                      {{ cache.info.rdb_last_bgsave_status }}
                    </div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">Key数量</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.dbSize" class="cell">{{ cache.dbSize }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">网络入口/出口</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div v-if="cache.info" class="cell">
                      {{ cache.info.instantaneous_input_kbps }}kps/{{ cache.info.instantaneous_output_kbps }}kps
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="table-panel">
          <template #header>
            <div class="toolbar-shell">
              <div class="table-heading">
                <h3>命令统计</h3>
              </div>
            </div>
          </template>
          <div class="el-table el-table--enable-row-hover el-table--medium cache-chart">
            <div ref="commandstats" style="height: 420px" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="table-panel">
          <template #header>
            <div class="toolbar-shell">
              <div class="table-heading">
                <h3>内存信息</h3>
              </div>
            </div>
          </template>
          <div class="el-table el-table--enable-row-hover el-table--medium cache-chart">
            <div ref="usedmemory" style="height: 420px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Cache" lang="ts">
import * as echarts from 'echarts';
import { getCache } from '@/api/monitor/cache';
import { CacheVO } from '@/api/monitor/cache/types';
import modal from '@/plugins/modal';

const cache = ref<Partial<CacheVO>>({});
const commandstats = ref();
const usedmemory = ref();
let commandstatsInstance: echarts.ECharts | undefined;
let usedmemoryInstance: echarts.ECharts | undefined;

const handleResize = () => {
  commandstatsInstance?.resize();
  usedmemoryInstance?.resize();
};

const disposeCharts = () => {
  commandstatsInstance?.dispose();
  usedmemoryInstance?.dispose();
  commandstatsInstance = undefined;
  usedmemoryInstance = undefined;
};

const getList = async () => {
  modal.loading('正在加载缓存监控数据，请稍候！');
  try {
    const res = await getCache();
    cache.value = res.data;
    disposeCharts();
    commandstatsInstance = echarts.init(commandstats.value, 'macarons');
    commandstatsInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b} : {c} ({d}%)'
      },
      series: [
        {
          name: '命令',
          type: 'pie',
          roseType: 'radius',
          radius: [15, 95],
          center: ['50%', '38%'],
          data: res.data.commandStats,
          animationEasing: 'cubicInOut',
          animationDuration: 1000
        }
      ]
    });
    usedmemoryInstance = echarts.init(usedmemory.value, 'macarons');
    usedmemoryInstance.setOption({
      tooltip: {
        formatter: '{b} <br/>{a} : ' + cache.value.info.used_memory_human
      },
      series: [
        {
          name: '峰值',
          type: 'gauge',
          min: 0,
          max: 1000,
          detail: {
            formatter: cache.value.info.used_memory_human
          },
          data: [
            {
              value: parseFloat(cache.value.info.used_memory_human),
              name: '内存消耗'
            }
          ]
        }
      ]
    });
  } finally {
    modal.closeLoading();
  }
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  getList();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  disposeCharts();
});
</script>

<style lang="scss" scoped>
.cache-grid {
  row-gap: 12px;
}

.cache-table {
  overflow: hidden;
  border: 1px solid var(--app-surface-border);
  border-radius: 10px;
}

.cache-table table {
  border-collapse: collapse;
}

.cache-chart {
  overflow: hidden;
  border-radius: 10px;
}
</style>
