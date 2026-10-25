<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">患者分析</h2>
        <p class="page-header__desc">新增患者趋势、性别构成与年龄段分布</p>
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
            <el-button v-perm="'report:export'" :icon="Download" @click="exportReport('excel')">
              导出 Excel
            </el-button>
            <el-button v-perm="'report:export'" :icon="Document" @click="exportReport('pdf')">
              导出 PDF
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">新增患者</p>
          <p class="stat-card__value">{{ report.total ?? 0 }}<small>人</small></p>
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
          <p class="stat-card__label">男性占比</p>
          <p class="stat-card__value">{{ genderPercent('男') }}<small>%</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">统计区间</p>
          <p class="stat-card__value small">{{ report.startDate || '-' }} 至 {{ report.endDate || '-' }}</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">新增患者趋势</h3>
      </div>
      <div class="chart-box" ref="trendRef"></div>
    </div>

    <div class="report-grid">
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">性别构成</h3>
        </div>
        <div class="chart-box chart-box--short" ref="genderRef"></div>
      </div>
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">年龄段分布</h3>
        </div>
        <div class="chart-box chart-box--short" ref="ageRef"></div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">年龄段明细</h3>
      </div>
      <div class="table-wrap">
        <el-table :data="report.ageRange || []" border>
          <el-table-column prop="name" label="年龄段" min-width="160" />
          <el-table-column prop="value" label="人数" width="130" align="right">
            <template #default="{ row }">{{ row.value ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="占比" min-width="240">
            <template #default="{ row }">
              <el-progress :percentage="percentOf(row.value)" :stroke-width="12" text-inside />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Document, Download, Refresh, Search } from '@element-plus/icons-vue'
import { reportPatient } from '@/api/business'
import { useReport } from '@/composables/useReport'
import { barOption, initChart, lineOption, pieOption, useChartResize } from '@/utils/echarts'

const { report, range, load, reset, exportReport } = useReport(reportPatient, 'patient', '患者分析')

const trendRef = ref<HTMLElement>()
const genderRef = ref<HTMLElement>()
const ageRef = ref<HTMLElement>()

let trendChart: any
let genderChart: any
let ageChart: any
const disposers: (() => void)[] = []

function genderPercent(name: string) {
  const list = (report.gender || []) as any[]
  const total = list.reduce((sum, item) => sum + Number(item.value || 0), 0)
  if (!total) return 0
  const hit = list.find((item) => item.name === name)
  return Math.round((Number(hit?.value || 0) / total) * 1000) / 10
}

function percentOf(value: unknown) {
  const list = (report.ageRange || []) as any[]
  const total = list.reduce((sum, item) => sum + Number(item.value || 0), 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(value || 0) / total) * 1000) / 10)
}

function renderCharts() {
  disposers.forEach((fn) => fn())
  disposers.length = 0
  const daily = (report.daily || []) as any[]
  const gender = (report.gender || []) as any[]
  const ageRange = (report.ageRange || []) as any[]

  trendChart = initChart(trendRef.value)
  if (trendChart) {
    trendChart.setOption(
      lineOption(
        daily.map((item) => item.name),
        [{ name: '新增患者', data: daily.map((item) => Number(item.value || 0)) }]
      )
    )
    disposers.push(useChartResize(trendChart))
  }

  genderChart = initChart(genderRef.value)
  if (genderChart) {
    genderChart.setOption(
      pieOption(
        gender.map((item) => item.name),
        gender.map((item) => Number(item.value || 0)),
        '人数'
      )
    )
    disposers.push(useChartResize(genderChart))
  }

  ageChart = initChart(ageRef.value)
  if (ageChart) {
    ageChart.setOption(
      barOption(
        ageRange.map((item) => item.name),
        ageRange.map((item) => Number(item.value || 0)),
        '人数'
      )
    )
    disposers.push(useChartResize(ageChart))
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
  genderChart?.dispose()
  ageChart?.dispose()
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
