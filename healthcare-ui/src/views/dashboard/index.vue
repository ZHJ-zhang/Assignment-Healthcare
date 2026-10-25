<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">工作台</h2>
        <p class="page-header__desc">{{ today }} · 医院运营概览</p>
      </div>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div v-for="item in statCards" :key="item.key" class="stat-card">
        <div class="stat-card__icon" :style="{ background: item.bg, color: item.color }">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-card__body">
          <p class="stat-card__label">{{ item.label }}</p>
          <p class="stat-card__value">
            {{ item.value }}
            <small v-if="item.suffix">{{ item.suffix }}</small>
          </p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">门诊量趋势</h3>
      </div>
      <div class="content-card__body">
        <div ref="trendRef" class="chart-box"></div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">各科室门诊量</h3>
        </div>
        <div class="content-card__body">
          <div ref="deptRef" class="chart-box chart-box--short"></div>
        </div>
      </div>

      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">收入构成</h3>
        </div>
        <div class="content-card__body">
          <div ref="payRef" class="chart-box chart-box--short"></div>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">科室收入排行</h3>
      </div>
      <div class="content-card__body">
        <el-table :data="incomeDept" size="small" border>
          <el-table-column prop="name" label="科室" min-width="160" />
          <el-table-column prop="value" label="收入金额" min-width="160" align="right">
            <template #default="{ row }">
              {{ formatAmount(row.value) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { dashboardSummary } from '@/api/business'
import {
  barOption,
  initChart,
  lineOption,
  pieOption,
  useChartResize
} from '@/utils/echarts'

const today = dayjs().format('YYYY年MM月DD日 dddd')

const summary = ref<any>({})
const trendRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()
const payRef = ref<HTMLElement>()

let trendChart: any
let deptChart: any
let payChart: any
const disposers: Array<() => void> = []

const statCards = computed(() => [
  {
    key: 'patientTotal',
    label: '患者总数',
    value: summary.value.patientTotal ?? 0,
    suffix: '人',
    icon: 'User',
    bg: '#e6f5f3',
    color: '#0f8a7e'
  },
  {
    key: 'patientToday',
    label: '今日新增患者',
    value: summary.value.patientToday ?? 0,
    suffix: '人',
    icon: 'Plus',
    bg: '#eaf2fb',
    color: '#2b7fd4'
  },
  {
    key: 'registrationToday',
    label: '今日挂号',
    value: summary.value.registrationToday ?? 0,
    suffix: '人次',
    icon: 'Tickets',
    bg: '#fdf1e3',
    color: '#d98324'
  },
  {
    key: 'inHospital',
    label: '当前在院',
    value: summary.value.inHospital ?? 0,
    suffix: '人',
    icon: 'OfficeBuilding',
    bg: '#fbe9ec',
    color: '#c8394a'
  }
])

const incomeDept = computed(() => (summary.value.income?.dept || []) as any[])

function formatAmount(value: unknown) {
  const num = Number(value || 0)
  return `￥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function toArray(list: any[] | undefined) {
  return list || []
}

function renderCharts() {
  const outpatient = summary.value.outpatient || {}
  const daily = toArray(outpatient.daily)
  const dept = toArray(outpatient.dept)

  trendChart = initChart(trendRef.value)
  if (trendChart) {
    trendChart.setOption(
      lineOption(
        daily.map((item: any) => item.name),
        [{ name: '门诊量', data: daily.map((item: any) => Number(item.value || 0)) }]
      )
    )
    disposers.push(useChartResize(trendChart))
  }

  deptChart = initChart(deptRef.value)
  if (deptChart) {
    deptChart.setOption(
      barOption(
        dept.map((item: any) => item.name),
        dept.map((item: any) => Number(item.value || 0)),
        '门诊量'
      )
    )
    disposers.push(useChartResize(deptChart))
  }

  const payType = toArray(summary.value.income?.payType)
  payChart = initChart(payRef.value)
  if (payChart) {
    payChart.setOption(
      pieOption(
        payType.map((item: any) => item.name),
        payType.map((item: any) => Number(item.value || 0)),
        '收入'
      )
    )
    disposers.push(useChartResize(payChart))
  }
}

async function loadAll() {
  summary.value = (await dashboardSummary()) || {}
  await nextTick()
  renderCharts()
}

onMounted(loadAll)
onUnmounted(() => {
  disposers.forEach((fn) => fn())
  trendChart?.dispose()
  deptChart?.dispose()
  payChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.dashboard-grid .content-card {
  margin-top: 0;
}
</style>
