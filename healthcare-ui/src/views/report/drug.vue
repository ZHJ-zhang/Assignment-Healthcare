<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">药品统计</h2>
        <p class="page-header__desc">处方药品销量与销售金额排行分析</p>
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
          <p class="stat-card__label">统计药品数</p>
          <p class="stat-card__value">{{ (report.ranking || []).length }}<small>种</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">销售总量</p>
          <p class="stat-card__value">{{ totalQuantity }}<small>件</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">销售总额</p>
          <p class="stat-card__value">{{ formatAmount(totalAmount) }}</p>
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
        <h3 class="content-card__title">药品销量 TOP20</h3>
      </div>
      <div class="chart-box" ref="quantityRef"></div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">药品销售金额 TOP20</h3>
      </div>
      <div class="chart-box chart-box--short" ref="amountRef"></div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">药品明细</h3>
      </div>
      <div class="table-wrap">
        <el-table :data="report.ranking || []" border>
          <el-table-column prop="drug_name" label="药品名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="total_quantity" label="销售数量" width="130" align="right">
            <template #default="{ row }">{{ row.total_quantity ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="销售金额" width="150" align="right">
            <template #default="{ row }">{{ formatAmount(row.total_amount) }}</template>
          </el-table-column>
          <el-table-column label="金额占比" min-width="240">
            <template #default="{ row }">
              <el-progress :percentage="percentOf(row.total_amount)" :stroke-width="12" text-inside />
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
import { reportDrug } from '@/api/business'
import { useReport } from '@/composables/useReport'
import { initChart, rankOption, useChartResize } from '@/utils/echarts'

const { report, range, load, reset, exportReport } = useReport(reportDrug, 'drug', '药品统计')

const quantityRef = ref<HTMLElement>()
const amountRef = ref<HTMLElement>()

let quantityChart: any
let amountChart: any
const disposers: (() => void)[] = []

const totalQuantity = computed(() =>
  ((report.ranking || []) as any[]).reduce((sum, row) => sum + Number(row.total_quantity || 0), 0)
)

const totalAmount = computed(() =>
  ((report.ranking || []) as any[]).reduce((sum, row) => sum + Number(row.total_amount || 0), 0)
)

function formatAmount(value: unknown) {
  const num = Number(value || 0)
  return `￥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function percentOf(value: unknown) {
  const total = Number(totalAmount.value || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(value || 0) / total) * 1000) / 10)
}

function renderCharts() {
  disposers.forEach((fn) => fn())
  disposers.length = 0
  const quantity = (report.quantity || []) as any[]
  const amount = (report.amount || []) as any[]

  quantityChart = initChart(quantityRef.value)
  if (quantityChart) {
    quantityChart.setOption(
      rankOption(
        quantity.map((item) => item.name),
        quantity.map((item) => Number(item.value || 0))
      )
    )
    disposers.push(useChartResize(quantityChart))
  }

  amountChart = initChart(amountRef.value)
  if (amountChart) {
    amountChart.setOption(
      rankOption(
        amount.map((item) => item.name),
        amount.map((item) => Number(item.value || 0))
      )
    )
    disposers.push(useChartResize(amountChart))
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
  quantityChart?.dispose()
  amountChart?.dispose()
})
</script>
