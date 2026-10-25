<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">门诊统计</h2>
        <p class="page-header__desc">按日、科室与医生维度分析门诊接诊量</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="统计区间">
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            @change="refresh()"
          />
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="refresh()">查询</el-button>
            <el-button :icon="Refresh" @click="refreshReset()">重置</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button v-perm="'report:export'" :icon="Download" @click="exportReport('excel')">导出 Excel</el-button>
            <el-button v-perm="'report:export'" :icon="Document" @click="exportReport('pdf')">导出 PDF</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">门诊总量</p>
          <p class="stat-card__value">{{ report.total ?? 0 }}<small>人次</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">统计天数</p>
          <p class="stat-card__value">{{ (report.daily || []).length }}<small>天</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">涉及科室</p>
          <p class="stat-card__value">{{ (report.dept || []).length }}<small>个</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">日均门诊量</p>
          <p class="stat-card__value">{{ dailyAverage }}<small>人次</small></p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">门诊量趋势</h3>
      </div>
      <div class="chart-box" ref="trendRef"></div>
    </div>

    <div class="report-grid">
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">科室门诊量</h3>
        </div>
        <div class="chart-box chart-box--short" ref="deptRef"></div>
      </div>
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">医生接诊量 TOP10</h3>
        </div>
        <div class="chart-box chart-box--short" ref="doctorRef"></div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">科室明细</h3>
      </div>
      <div class="table-wrap">
        <el-table :data="report.dept || []" border>
          <el-table-column prop="name" label="科室" min-width="160" />
          <el-table-column prop="value" label="门诊人次" width="140" align="right">
            <template #default="{ row }">{{ row.value ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="占比" min-width="220">
            <template #default="{ row }">
              <el-progress
                :percentage="percentOf(row.value)"
                :stroke-width="12"
                text-inside
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Document, Download, Refresh, Search } from '@element-plus/icons-vue'
import { reportOutpatient } from '@/api/business'
import { barOption, initChart, lineOption, rankOption, useChartResize } from '@/utils/echarts'
import { useReport } from '@/composables/useReport'

const { report, range, load, reset, exportReport } = useReport(
  reportOutpatient,
  'outpatient',
  '门诊统计'
)

const trendRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()
const doctorRef = ref<HTMLElement>()

let trendChart: any
let deptChart: any
let doctorChart: any
const disposers: (() => void)[] = []

const dailyAverage = computed(() => {
  const daily = (report.daily || []) as any[]
  if (!daily.length) return 0
  const sum = daily.reduce((acc, item) => acc + Number(item.value || 0), 0)
  return Math.round((sum / daily.length) * 10) / 10
})

function percentOf(value: unknown) {
  const total = Number(report.total || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(value || 0) / total) * 1000) / 10)
}

function renderCharts() {
  disposers.forEach((fn) => fn())
  disposers.length = 0
  const daily = (report.daily || []) as any[]
  const dept = (report.dept || []) as any[]
  const doctor = (report.doctor || []) as any[]

  trendChart = initChart(trendRef.value)
  if (trendChart) {
    trendChart.setOption(
      lineOption(
        daily.map((item) => item.name),
        [{ name: '门诊量', data: daily.map((item) => Number(item.value || 0)) }]
      )
    )
    disposers.push(useChartResize(trendChart))
  }

  deptChart = initChart(deptRef.value)
  if (deptChart) {
    deptChart.setOption(
      barOption(
        dept.map((item) => item.name),
        dept.map((item) => Number(item.value || 0)),
        '门诊量'
      )
    )
    disposers.push(useChartResize(deptChart))
  }

  doctorChart = initChart(doctorRef.value)
  if (doctorChart) {
    doctorChart.setOption(
      rankOption(
        doctor.map((item) => item.name),
        doctor.map((item) => Number(item.value || 0))
      )
    )
    disposers.push(useChartResize(doctorChart))
  }
}

async function refresh() {
  await load()
  await nextTick()
  renderCharts()
}

async function refreshReset() {
  await reset()
  await nextTick()
  renderCharts()
}

onMounted(refresh)

onUnmounted(() => {
  disposers.forEach((fn) => fn())
  trendChart?.dispose()
  deptChart?.dispose()
  doctorChart?.dispose()
})
</script>

<style scoped>
.report-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.report-grid .content-card {
  margin-top: 0;
}
</style>
