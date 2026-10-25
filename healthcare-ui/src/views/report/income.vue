<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">收入统计</h2>
        <p class="page-header__desc">按日、支付方式与科室分析医院收入结构</p>
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
          <p class="stat-card__label">收入总额</p>
          <p class="stat-card__value">{{ formatAmount(report.totalAmount) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">收费笔数</p>
          <p class="stat-card__value">{{ report.totalCount ?? 0 }}<small>笔</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">单均收入</p>
          <p class="stat-card__value">{{ formatAmount(avgAmount) }}</p>
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
        <h3 class="content-card__title">收入趋势</h3>
      </div>
      <div class="chart-box" ref="trendRef"></div>
    </div>

    <div class="report-grid">
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">支付方式构成</h3>
        </div>
        <div class="chart-box chart-box--short" ref="payRef"></div>
      </div>
      <div class="content-card">
        <div class="content-card__header">
          <h3 class="content-card__title">科室收入排行</h3>
        </div>
        <div class="chart-box chart-box--short" ref="deptRef"></div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">科室收入明细</h3>
      </div>
      <div class="table-wrap">
        <el-table :data="report.dept || []" border>
          <el-table-column prop="name" label="科室" min-width="160" />
          <el-table-column label="收入金额" width="150" align="right">
            <template #default="{ row }">{{ formatAmount(row.value) }}</template>
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
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Document, Download, Refresh, Search } from '@element-plus/icons-vue'
import { reportIncome } from '@/api/business'
import { useReport } from '@/composables/useReport'
import { barOption, initChart, lineOption, pieOption, useChartResize } from '@/utils/echarts'

const { report, range, load, reset, exportReport } = useReport(reportIncome, 'income', '收入统计')

const trendRef = ref<HTMLElement>()
const payRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()

let trendChart: any
let payChart: any
let deptChart: any
const disposers: (() => void)[] = []

const avgAmount = computed(() => {
  const count = Number(report.totalCount || 0)
  if (!count) return 0
  return Number(report.totalAmount || 0) / count
})

function formatAmount(value: unknown) {
  const num = Number(value || 0)
  return `￥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function percentOf(value: unknown) {
  const total = Number(report.totalAmount || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(value || 0) / total) * 1000) / 10)
}

function renderCharts() {
  disposers.forEach((fn) => fn())
  disposers.length = 0
  const daily = (report.daily || []) as any[]
  const payType = (report.payType || []) as any[]
  const dept = (report.dept || []) as any[]

  trendChart = initChart(trendRef.value)
  if (trendChart) {
    trendChart.setOption(
      lineOption(
        daily.map((item) => item.name),
        [{ name: '收入', data: daily.map((item) => Number(item.value || 0)) }]
      )
    )
    disposers.push(useChartResize(trendChart))
  }

  payChart = initChart(payRef.value)
  if (payChart) {
    payChart.setOption(
      pieOption(
        payType.map((item) => item.name),
        payType.map((item) => Number(item.value || 0)),
        '收入'
      )
    )
    disposers.push(useChartResize(payChart))
  }

  deptChart = initChart(deptRef.value)
  if (deptChart) {
    deptChart.setOption(
      barOption(
        dept.map((item) => item.name),
        dept.map((item) => Number(item.value || 0)),
        '收入'
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
  payChart?.dispose()
  deptChart?.dispose()
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
