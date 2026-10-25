<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">住院统计</h2>
        <p class="page-header__desc">分析入出院趋势与各科室在院人数</p>
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
          <p class="stat-card__label">累计入院</p>
          <p class="stat-card__value">{{ report.admissionTotal ?? 0 }}<small>人次</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">累计出院</p>
          <p class="stat-card__value">{{ report.dischargeTotal ?? 0 }}<small>人次</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">当前在院</p>
          <p class="stat-card__value">{{ report.inHospitalTotal ?? 0 }}<small>人</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">出院率</p>
          <p class="stat-card__value">{{ dischargeRate }}<small>%</small></p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">入出院趋势</h3>
      </div>
      <div class="chart-box" ref="trendRef"></div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">各科室在院人数</h3>
      </div>
      <div class="chart-box chart-box--short" ref="deptRef"></div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">科室明细</h3>
      </div>
      <div class="table-wrap">
        <el-table :data="report.dept || []" border>
          <el-table-column prop="name" label="科室" min-width="160" />
          <el-table-column prop="value" label="在院人数" width="140" align="right">
            <template #default="{ row }">{{ row.value ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="占在院总数" min-width="240">
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
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Document, Download, Refresh, Search } from '@element-plus/icons-vue'
import { reportInpatient } from '@/api/business'
import { useReport } from '@/composables/useReport'
import { barOption, initChart, lineOption, useChartResize } from '@/utils/echarts'

const { report, range, load, reset, exportReport } = useReport(
  reportInpatient,
  'inpatient',
  '住院统计'
)

const trendRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()

let trendChart: any
let deptChart: any
const disposers: (() => void)[] = []

const dischargeRate = computed(() => {
  const admission = Number(report.admissionTotal || 0)
  if (!admission) return 0
  return Math.round((Number(report.dischargeTotal || 0) / admission) * 1000) / 10
})

function percentOf(value: unknown) {
  const total = Number(report.inHospitalTotal || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(value || 0) / total) * 1000) / 10)
}

function renderCharts() {
  disposers.forEach((fn) => fn())
  disposers.length = 0
  const admission = (report.admission || []) as any[]
  const discharge = (report.discharge || []) as any[]
  const dept = (report.dept || []) as any[]

  trendChart = initChart(trendRef.value)
  if (trendChart) {
    trendChart.setOption(
      lineOption(
        admission.map((item) => item.name),
        [
          { name: '入院', data: admission.map((item) => Number(item.value || 0)) },
          { name: '出院', data: discharge.map((item) => Number(item.value || 0)) }
        ]
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
        '在院人数'
      )
    )
    disposers.push(useChartResize(deptChart))
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
})
</script>
