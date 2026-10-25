<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">发票管理</h2>
        <p class="page-header__desc">收费单开票、发票查询与作废</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="发票号">
          <el-input
            v-model="query.keyword"
            placeholder="按发票号码搜索"
            clearable
            style="width: 190px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="已开票" :value="1" />
            <el-option label="已作废" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="search()">查询</el-button>
            <el-button :icon="Refresh" @click="reset()">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-perm="'charge:invoice:print'" type="primary" :icon="Printer" @click="openPrint()">
          开具发票
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="invoiceNo" label="发票号码" width="180" />
          <el-table-column prop="invoiceCode" label="发票代码" width="180" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="title" label="抬头" width="150" />
          <el-table-column prop="taxNo" label="税号" width="160">
            <template #default="{ row }">{{ row.taxNo || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.amount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="票种" width="110" align="center">
            <template #default="{ row }">{{ invoiceTypeName(row.invoiceType) }}</template>
          </el-table-column>
          <el-table-column prop="invoiceTime" label="开票时间" width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '已开票' : '已作废' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="voidReason" label="作废原因" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.voidReason || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'charge:invoice:void'"
                link
                type="danger"
                :disabled="row.status !== 1"
                @click="handleVoid(row)"
              >
                作废
              </el-button>
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
    </div>

    <el-dialog v-model="printVisible" title="开具发票" width="560px">
      <el-form ref="printFormRef" :model="printForm" :rules="printRules" label-width="92px">
        <el-form-item label="收费单" prop="chargeId">
          <el-select v-model="printForm.chargeId" placeholder="请选择已支付收费单" filterable style="width: 100%">
            <el-option
              v-for="item in chargeOptions"
              :key="item.id"
              :label="`${item.chargeNo} · ￥${Number(item.actualAmount || 0).toFixed(2)}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发票抬头">
          <el-input v-model="printForm.title" placeholder="个人或单位名称" />
        </el-form-item>
        <el-form-item label="纳税人识别号">
          <el-input v-model="printForm.taxNo" placeholder="单位发票时填写" />
        </el-form-item>
        <el-form-item label="票种" prop="invoiceType">
          <el-select v-model="printForm.invoiceType" placeholder="请选择" style="width: 100%">
            <el-option label="普通发票" :value="1" />
            <el-option label="增值税专用发票" :value="2" />
            <el-option label="电子发票" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="printVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPrint">开具</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Printer, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { pageCharges, pageInvoices, printInvoice, voidInvoice } from '@/api/business'
import { usePage } from '@/composables/usePage'
import { loadPatientOptions, patientName } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageInvoices, {
  keyword: '',
  status: undefined
})

const chargeOptions = ref<any[]>([])

const printVisible = ref(false)
const saving = ref(false)
const printFormRef = ref<FormInstance>()
const printForm = reactive<any>({
  chargeId: undefined,
  title: '',
  taxNo: '',
  invoiceType: 1
})

const printRules: FormRules = {
  chargeId: [{ required: true, message: '请选择收费单', trigger: 'change' }],
  invoiceType: [{ required: true, message: '请选择票种', trigger: 'change' }]
}

function invoiceTypeName(type: number) {
  return { 1: '普通发票', 2: '增值税专用发票', 3: '电子发票' }[type] ?? '其他'
}

async function loadCharges() {
  const result: any = await pageCharges({ pageNum: 1, pageSize: 300, payStatus: 1 })
  chargeOptions.value = result?.list || []
}

function openPrint() {
  Object.assign(printForm, {
    chargeId: undefined,
    title: '',
    taxNo: '',
    invoiceType: 1
  })
  printVisible.value = true
  printFormRef.value?.clearValidate()
}

async function submitPrint() {
  const valid = await printFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await printInvoice({
      chargeId: printForm.chargeId,
      title: printForm.title,
      taxNo: printForm.taxNo,
      invoiceType: printForm.invoiceType
    })
    ElMessage.success('发票开具成功')
    printVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleVoid(row: any) {
  const result = await ElMessageBox.prompt('请填写作废原因', '作废发票', {
    confirmButtonText: '确定作废',
    cancelButtonText: '取消',
    inputPlaceholder: '作废原因',
    inputValidator: (value) => (value && value.trim() ? true : '请输入作废原因')
  })
  await voidInvoice(row.id, result.value)
  ElMessage.success('发票已作废')
  load()
}

onMounted(async () => {
  await loadPatientOptions()
  await loadCharges()
  load()
})
</script>
