<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">库存管理</h2>
        <p class="page-header__desc">按批号管理药品出入库、库存与效期</p>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="库存明细" name="stock">
          <el-form :model="query" inline class="search-bar">
            <el-form-item label="药品">
              <el-select
                v-model="query.drugId"
                placeholder="全部药品"
                clearable
                filterable
                style="width: 220px"
                @change="search()"
              >
                <el-option
                  v-for="item in drugOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="库房">
              <el-select
                v-model="query.warehouseId"
                placeholder="全部库房"
                clearable
                style="width: 180px"
                @change="search()"
              >
                <el-option
                  v-for="item in warehouseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="批号">
              <el-input
                v-model="query.keyword"
                placeholder="按批号搜索"
                clearable
                style="width: 180px"
                @keyup.enter="search()"
                @clear="search()"
              />
            </el-form-item>
            <el-form-item>
              <div class="search-bar__actions">
                <el-button type="primary" :icon="Search" @click="search()">查询</el-button>
                <el-button :icon="Refresh" @click="reset()">重置</el-button>
              </div>
            </el-form-item>
          </el-form>

          <div class="toolbar">
            <el-button v-perm="'drug:stock:in'" type="primary" :icon="Download" @click="openStock('in')">
              药品入库
            </el-button>
            <el-button v-perm="'drug:stock:out'" type="warning" :icon="Upload" @click="openStock('out')">
              药品出库
            </el-button>
          </div>

          <div class="table-wrap">
            <el-table v-loading="loading" :data="list" border>
              <el-table-column prop="drugName" label="药品名称" min-width="150" />
              <el-table-column prop="batchNo" label="批号" width="130" />
              <el-table-column label="库房" width="140">
                <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="库存数量" width="100" align="right">
                <template #default="{ row }">
                  <span :class="{ 'text-danger': isLow(row) }">{{ row.quantity ?? 0 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="warnNum" label="预警值" width="90" align="right" />
              <el-table-column prop="lockQuantity" label="锁定量" width="90" align="right">
                <template #default="{ row }">{{ row.lockQuantity ?? 0 }}</template>
              </el-table-column>
              <el-table-column prop="produceDate" label="生产日期" width="120" />
              <el-table-column prop="expireDate" label="有效期至" width="120">
                <template #default="{ row }">
                  <el-tag v-if="isExpired(row)" type="danger" size="small">已过期</el-tag>
                  <span v-else>{{ row.expireDate || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag v-if="isLow(row)" type="warning" size="small">库存不足</el-tag>
                  <el-tag v-else type="success" size="small">正常</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="query.pageNum"
              v-model:page-size="query.pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="load()"
              @size-change="search()"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="库存预警" name="warn">
          <div class="table-wrap">
            <el-table v-loading="warnLoading" :data="warnList" border>
              <el-table-column prop="drugName" label="药品名称" min-width="150" />
              <el-table-column prop="batchNo" label="批号" width="130" />
              <el-table-column prop="quantity" label="当前库存" width="110" align="right" />
              <el-table-column prop="warnNum" label="预警值" width="100" align="right" />
              <el-table-column label="库房" min-width="140">
                <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="效期预警" name="expire">
          <div class="toolbar">
            <span class="form-tip">提示天数</span>
            <el-input-number v-model="warnDays" :min="1" :max="365" size="small" style="width: 110px" />
            <el-button type="primary" :icon="Search" @click="loadExpire()">刷新</el-button>
          </div>
          <div class="table-wrap">
            <el-table v-loading="expireLoading" :data="expireList" border>
              <el-table-column prop="drugName" label="药品名称" min-width="150" />
              <el-table-column prop="batchNo" label="批号" width="130" />
              <el-table-column prop="quantity" label="库存数量" width="110" align="right" />
              <el-table-column prop="produceDate" label="生产日期" width="120" />
              <el-table-column prop="expireDate" label="有效期至" width="120" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="isExpired(row)" type="danger" size="small">已过期</el-tag>
                  <el-tag v-else type="warning" size="small">即将到期</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="出入库流水" name="record">
          <div class="table-wrap">
            <el-table v-loading="recordLoading" :data="recordList" border>
              <el-table-column prop="drugName" label="药品名称" min-width="150" />
              <el-table-column label="类型" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.recordType === 1 ? 'success' : 'warning'" size="small">
                    {{ recordTypeName(row.recordType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="90" align="right" />
              <el-table-column prop="batchNo" label="批号" width="130" />
              <el-table-column label="库房" min-width="130">
                <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
              </el-table-column>
              <el-table-column prop="bizNo" label="关联单号" width="150" />
              <el-table-column prop="createTime" label="操作时间" width="170" />
            </el-table>
          </div>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="recordQuery.pageNum"
              v-model:page-size="recordQuery.pageSize"
              :total="recordTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadRecords()"
              @size-change="loadRecords()"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="stockVisible"
      :title="stockMode === 'in' ? '药品入库' : '药品出库'"
      width="620px"
    >
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="92px">
        <el-form-item label="药品" prop="drugId">
          <el-select v-model="stockForm.drugId" placeholder="请选择药品" filterable style="width: 100%">
            <el-option
              v-for="item in drugOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="批号" prop="batchNo">
              <el-input v-model="stockForm.batchNo" placeholder="批号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="stockForm.quantity" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库房" prop="warehouseId">
              <el-select v-model="stockForm.warehouseId" placeholder="请选择库房" style="width: 100%">
                <el-option
                  v-for="item in warehouseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预警值">
              <el-input-number v-model="stockForm.warnNum" :min="0" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产日期">
              <el-date-picker v-model="stockForm.produceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期">
              <el-date-picker v-model="stockForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联单号">
              <el-input v-model="stockForm.bizNo" placeholder="如处方号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="stockForm.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="stockSaving" @click="submitStock">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Download, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  listWarehouses,
  pageDrugs,
  pageStock,
  pageStockRecords,
  stockExpiring,
  stockIn,
  stockOut,
  stockWarnings
} from '@/api/business'
import { usePage } from '@/composables/usePage'

const activeTab = ref('stock')

const { list, total, loading, query, load, search, reset } = usePage(pageStock, {
  drugId: undefined,
  warehouseId: undefined,
  keyword: ''
})

const drugOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])

const warnList = ref<any[]>([])
const warnLoading = ref(false)
const expireList = ref<any[]>([])
const expireLoading = ref(false)
const warnDays = ref(90)

const recordList = ref<any[]>([])
const recordTotal = ref(0)
const recordLoading = ref(false)
const recordQuery = reactive({ pageNum: 1, pageSize: 10 })

const stockVisible = ref(false)
const stockMode = ref<'in' | 'out'>('in')
const stockSaving = ref(false)
const stockFormRef = ref<FormInstance>()
const stockForm = reactive<any>({
  drugId: undefined,
  batchNo: '',
  quantity: 1,
  warehouseId: undefined,
  produceDate: '',
  expireDate: '',
  warnNum: 0,
  bizNo: '',
  remark: ''
})

const stockRules: FormRules = {
  drugId: [{ required: true, message: '请选择药品', trigger: 'change' }],
  batchNo: [{ required: true, message: '请输入批号', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择库房', trigger: 'change' }]
}

const today = computed(() => new Date().toISOString().slice(0, 10))

function isLow(row: any) {
  return Number(row.quantity ?? 0) <= Number(row.warnNum ?? 0)
}

function isExpired(row: any) {
  return !!row.expireDate && row.expireDate < today.value
}

function recordTypeName(type: number) {
  return { 1: '入库', 2: '出库', 3: '盘点', 4: '退药' }[type] ?? '其他'
}

function warehouseName(id: unknown) {
  const hit = warehouseOptions.value.find((item) => item.value === Number(id))
  return hit ? hit.label : '-'
}

async function loadOptions() {
  const [drugs, warehouses] = await Promise.all([
    pageDrugs({ pageNum: 1, pageSize: 500, status: 1 }),
    listWarehouses()
  ])
  drugOptions.value = (drugs?.list || []).map((item: any) => ({
    value: item.id,
    label: `${item.drugName} (${item.spec || '无规格'})`
  }))
  warehouseOptions.value = (warehouses || []).map((item: any) => ({
    value: item.id,
    label: item.warehouseName
  }))
}

async function loadWarn() {
  warnLoading.value = true
  try {
    warnList.value = (await stockWarnings()) || []
  } finally {
    warnLoading.value = false
  }
}

async function loadExpire() {
  expireLoading.value = true
  try {
    expireList.value = (await stockExpiring(warnDays.value)) || []
  } finally {
    expireLoading.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const result: any = await pageStockRecords(recordQuery)
    recordList.value = result?.list || []
    recordTotal.value = Number(result?.total || 0)
  } finally {
    recordLoading.value = false
  }
}

function openStock(mode: 'in' | 'out') {
  stockMode.value = mode
  Object.assign(stockForm, {
    drugId: undefined,
    batchNo: '',
    quantity: 1,
    warehouseId: warehouseOptions.value[0]?.value,
    produceDate: '',
    expireDate: '',
    warnNum: 0,
    bizNo: '',
    remark: ''
  })
  stockVisible.value = true
  stockFormRef.value?.clearValidate()
}

async function submitStock() {
  const valid = await stockFormRef.value?.validate().catch(() => false)
  if (!valid) return
  stockSaving.value = true
  try {
    const payload = { ...stockForm }
    if (stockMode.value === 'in') {
      await stockIn(payload)
      ElMessage.success('入库成功')
    } else {
      await stockOut(payload)
      ElMessage.success('出库成功')
    }
    stockVisible.value = false
    load()
    loadRecords()
    loadWarn()
  } finally {
    stockSaving.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'warn') loadWarn()
  if (tab === 'expire') loadExpire()
  if (tab === 'record') loadRecords()
})

onMounted(async () => {
  await loadOptions()
  load()
})
</script>

<style scoped>
.text-danger {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
