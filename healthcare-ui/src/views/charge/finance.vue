<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">财务对账</h2>
        <p class="page-header__desc">收费收入日结、月结与医保结算</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="结算周期">
          <el-radio-group v-model="query.mode" @change="onModeChange">
            <el-radio-button value="daily">日结</el-radio-button>
            <el-radio-button value="monthly">月结</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="query.mode === 'daily'" label="结算日期">
          <el-date-picker
            v-model="query.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            @change="load()"
          />
        </el-form-item>
        <el-form-item v-else label="结算月份">
          <el-date-picker
            v-model="query.month"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            @change="load()"
          />
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="load()">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">收入总额</p>
          <p class="stat-card__value">{{ formatAmount(summary.totalAmount) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">收费笔数</p>
          <p class="stat-card__value">{{ summary.totalCount ?? 0 }}<small>笔</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">统计区间</p>
          <p class="stat-card__value small">{{ summary.startDate || '-' }} 至 {{ summary.endDate || '-' }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__body">
          <p class="stat-card__label">客单价</p>
          <p class="stat-card__value">{{ formatAmount(avgAmount) }}</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">收入趋势</h3>
      </div>
      <div class="chart-box" ref="trendRef"></div>
    </div>

    <div class="finance-grid">
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
        <h3 class="content-card__title">医保结算</h3>
      </div>
      <el-form :model="settleForm" inline label-width="92px">
        <el-form-item label="住院单号">
          <el-select v-model="settleForm.admissionId" placeholder="选择在院住院单" filterable style="width: 260px">
            <el-option
              v-for="item in admissionOptions"
              :key="item.id"
              :label="`${item.admissionNo} · ${patientName(item.patientId) || ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="医保金额">
          <el-input-number v-model="settleForm.insuranceAmount" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="自付金额">
          <el-input-number v-model="settleForm.selfAmount" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item>
          <el-button v-perm="'charge:finance:daily'" type="primary" :loading="settling" @click="submitSettle">
            提交结算
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { financeDaily, financeMonthly, insuranceSettle } from '@/api/business'
import { pageAdmissions } from '@/api/medical'
import { barOption, initChart, lineOption, pieOption, useChartResize } from '@/utils/echarts'
import { loadPatientOptions, patientName } from '@/utils/lookup'

const query = reactive<any>({
  mode: 'daily',
  date: new Date().toISOString().slice(0, 10),
  month: new Date().toISOString().slice(0, 7)
})

const summary = reactive<any>({})
const admissionOptions = ref<any[]>([])

const settleForm = reactive<any>({
  admissionId: undefined,
  insuranceAmount: 0,
  selfAmount: 0
})
const settling = ref(false)

const trendRef = ref<HTMLElement>()
const payRef = ref<HTMLElement>()
const deptRef = ref<HTMLElement>()

let trendChart: any
let payChart: any
let deptChart: any
const disposers: (() => void)[] = []

const avgAmount = computed(() => {
  const count = Number(summary.totalCount || 0)
  if (!count) return 0
  return Number(summary.totalAmount || 0) / count
})

function formatAmount(value: unknown) {
  const num = Number(value || 0)
  return `￥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function onModeChange() {
  load()
}

function reset() {
  query.mode = 'daily'
  query.date = new Date().toISOString().slice(0, 10)
  query.month = new Date().toISOString().slice(0, 7)
  load()
}

function renderCharts() {
  const daily = (summary.dailyList || []) as any[]
  const payType = (summary.payTypeList || []) as any[]
  const dept = (summary.deptList || []) as any[]

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

async function load() {
  const result: any =
    query.mode === 'daily'
      ? await financeDaily({ date: query.date })
      : await financeMonthly({ month: query.month })
  Object.assign(summary, result || {})
  await nextTick()
  renderCharts()
}

async function loadAdmissions() {
  const result: any = await pageAdmissions({ pageNum: 1, pageSize: 300, status: 0 })
  admissionOptions.value = result?.list || []
}

async function submitSettle() {
  if (!settleForm.admissionId) {
    ElMessage.warning('请选择住院单')
    return
  }
  settling.value = true
  try {
    await insuranceSettle({
      admissionId: settleForm.admissionId,
      insuranceAmount: settleForm.insuranceAmount,
      selfAmount: settleForm.selfAmount
    })
    ElMessage.success('医保结算成功')
    load()
  } finally {
    settling.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadPatientOptions(), loadAdmissions()])
  load()
})

onUnmounted(() => {
  disposers.forEach((fn) => fn())
  trendChart?.dispose()
  payChart?.dispose()
  deptChart?.dispose()
})
</script>

<style scoped>
.finance-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.finance-grid .content-card {
  margin-top: 0;
}
</style>
