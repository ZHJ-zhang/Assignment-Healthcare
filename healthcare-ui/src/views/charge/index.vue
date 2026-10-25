<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">收费管理</h2>
        <p class="page-header__desc">门诊与住院收费登记、收费单查询</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="收费单号">
          <el-input
            v-model="query.keyword"
            placeholder="按收费单号搜索"
            clearable
            style="width: 190px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="科室">
          <el-select
            v-model="query.deptId"
            placeholder="全部科室"
            clearable
            filterable
            style="width: 160px"
            @change="search()"
          >
            <el-option
              v-for="item in deptOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="query.payType" placeholder="全部" clearable style="width: 130px" @change="search()">
            <el-option label="现金" :value="1" />
            <el-option label="微信" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="医保" :value="4" />
            <el-option label="银行卡" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.payStatus" placeholder="全部" clearable style="width: 130px" @change="search()">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已退费" :value="2" />
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
        <el-button v-perm="'charge:add'" type="primary" :icon="Plus" @click="openDialog()">新增收费</el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="chargeNo" label="收费单号" width="170" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="收费类型" width="100" align="center">
            <template #default="{ row }">{{ visitTypeName(row.visitType) }}</template>
          </el-table-column>
          <el-table-column label="总金额" width="110" align="right">
            <template #default="{ row }">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="优惠" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.discountAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="实收金额" width="110" align="right">
            <template #default="{ row }">
              <span class="amount-strong">￥{{ Number(row.actualAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="支付方式" width="100" align="center">
            <template #default="{ row }">{{ payTypeName(row.payType) }}</template>
          </el-table-column>
          <el-table-column prop="chargeTime" label="收费时间" width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusTag(row.payStatus).type" size="small">
                {{ payStatusTag(row.payStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">明细</el-button>
              <el-button
                v-perm="'charge:refund'"
                link
                type="danger"
                :disabled="row.payStatus !== 1"
                @click="openRefund(row)"
              >
                退费
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

    <el-dialog v-model="dialogVisible" title="新增收费单" width="820px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="患者" prop="patientId">
              <el-select v-model="form.patientId" placeholder="请选择患者" filterable style="width: 100%" @change="onPatientChange">
                <el-option
                  v-for="item in patientOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="科室">
              <el-select v-model="form.deptId" placeholder="请选择科室" clearable filterable style="width: 100%">
                <el-option
                  v-for="item in deptOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <div v-if="currentVisit" class="field-tip">
                已按本次就诊带出（{{ currentVisit.regNo }} · {{ currentVisit.queueNo }} 号）
              </div>
              <div v-else-if="form.patientId" class="field-tip field-tip--warn">
                该患者今日无挂号记录，科室请手动选择
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收费类型" prop="visitType">
              <el-select v-model="form.visitType" placeholder="请选择" style="width: 100%">
                <el-option label="挂号费" :value="1" />
                <el-option label="门诊费" :value="2" />
                <el-option label="住院费" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="支付方式" prop="payType">
              <el-select v-model="form.payType" placeholder="请选择" style="width: 100%">
                <el-option label="现金" :value="1" />
                <el-option label="微信" :value="2" />
                <el-option label="支付宝" :value="3" />
                <el-option label="医保" :value="4" />
                <el-option label="银行卡" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优惠金额">
              <el-input-number
                v-model="form.discountAmount"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联处方">
              <el-select v-model="form.prescriptionId" placeholder="可选" clearable filterable style="width: 100%" @change="onPrescriptionChange">
                <el-option
                  v-for="item in prescriptionOptions"
                  :key="item.id"
                  :label="`${item.prescNo} · ${patientName(item.patientId) || ''}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-alert
          v-if="form.patientId && !prescriptionOptions.length && chargedPrescriptions.length"
          class="presc-alert"
          type="info"
          :closable="false"
          show-icon
          title="该患者的处方都已收费，无需重复收费，可直接手工选择收费项目"
        />

        <el-form-item label="收费明细">
          <div class="item-picker">
            <el-select
              v-model="pickerItemId"
              placeholder="选择收费项目"
              filterable
              style="width: 260px"
              @change="addItem"
            >
              <el-option
                v-for="item in feeItemOptions"
                :key="item.id"
                :label="`${item.itemName} ￥${Number(item.price || 0).toFixed(2)}`"
                :value="item.id"
              />
            </el-select>
          </div>
          <el-table :data="form.items" border size="small" style="width: 100%">
            <el-table-column prop="itemName" label="项目名称" min-width="160" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">{{ itemTypeName(row.itemType) }}</template>
            </el-table-column>
            <el-table-column label="单价" width="110" align="right">
              <template #default="{ row }">￥{{ Number(row.price || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="数量" width="140" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="1" :precision="0" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }">
                ￥{{ (Number(row.price || 0) * Number(row.quantity || 0)).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="form.items.splice($index, 1)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>

        <el-form-item label="检查申请">
          <div v-if="!examApplyOptions.length" class="form-tip">
            该患者暂无待缴费的检查申请单（医生开单后会自动出现在这里）
          </div>
          <el-checkbox-group v-else v-model="form.examApplyIds" style="width: 100%">
            <div class="exam-apply-list">
              <el-checkbox v-for="apply in examApplyOptions" :key="apply.id" :value="apply.id">
                {{ apply.itemName || '检查费' }}
                <span class="form-tip">
                  （{{ apply.applyNo }} · {{ apply.examType === 2 ? '检查' : '检验' }} ·
                  ￥{{ Number(apply.amount || 0).toFixed(2) }}）
                </span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-form-item>

        <div class="charge-total">
          合计金额：￥{{ totalAmount.toFixed(2) }}
          <span class="form-tip">实收 ￥{{ (totalAmount - Number(form.discountAmount || 0)).toFixed(2) }}</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定收费</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="收费明细" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="收费单号">{{ detail.charge?.chargeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">
          {{ patientName(detail.charge?.patientId) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="科室">{{ deptName(detail.charge?.deptId) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收费类型">
          {{ visitTypeName(detail.charge?.visitType) }}
        </el-descriptions-item>
        <el-descriptions-item label="总金额">
          ￥{{ Number(detail.charge?.totalAmount || 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="实收金额">
          ￥{{ Number(detail.charge?.actualAmount || 0).toFixed(2) }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.items || []" border size="small" style="margin-top: 14px">
        <el-table-column prop="itemName" label="项目名称" min-width="160" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">{{ itemTypeName(row.itemType) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column label="单价" width="100" align="right">
          <template #default="{ row }">￥{{ Number(row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="110" align="right">
          <template #default="{ row }">￥{{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="refundVisible" title="申请退费" width="520px">
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="92px">
        <el-form-item label="收费单号">
          <el-input v-model="refundForm.chargeNo" disabled />
        </el-form-item>
        <el-form-item label="退费金额" prop="amount">
          <el-input-number
            v-model="refundForm.amount"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退费类型" prop="refundType">
          <el-select v-model="refundForm.refundType" placeholder="请选择" style="width: 100%">
            <el-option label="部分退费" :value="1" />
            <el-option label="全部退费" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="退费原因">
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" placeholder="退费原因说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute } from 'vue-router'
import { applyRefund, chargeDetail, createCharge, listFeeItems, pageCharges, pageExamApplies } from '@/api/business'
import { getPrescription, pagePrescriptions, pageRegistrations } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { deptName, loadDeptOptions, loadPatientOptions, patientName, type LookupOption } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageCharges, {
  keyword: '',
  deptId: undefined,
  payType: undefined,
  payStatus: undefined
})

const route = useRoute()

const patientOptions = ref<LookupOption[]>([])
const deptOptions = ref<LookupOption[]>([])
const feeItemOptions = ref<any[]>([])
const prescriptionOptions = ref<any[]>([])
const chargedPrescriptions = ref<any[]>([])
const examApplyOptions = ref<any[]>([])
const currentVisit = ref<any>(null)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const pickerItemId = ref<number>()

const form = reactive<any>({
  patientId: undefined,
  deptId: undefined,
  prescriptionId: undefined,
  examApplyIds: [] as any[],
  visitType: 2,
  payType: 1,
  discountAmount: 0,
  items: [] as any[]
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择收费类型', trigger: 'change' }],
  payType: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = reactive<any>({})

const refundVisible = ref(false)
const refundFormRef = ref<FormInstance>()
const refundForm = reactive<any>({
  chargeId: undefined,
  chargeNo: '',
  amount: 0,
  refundType: 1,
  reason: ''
})

const refundRules: FormRules = {
  amount: [{ required: true, message: '请输入退费金额', trigger: 'blur' }],
  refundType: [{ required: true, message: '请选择退费类型', trigger: 'change' }]
}

const totalAmount = computed(
  () =>
    form.items.reduce((sum: number, item: any) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0) +
    examApplyOptions.value
      .filter((apply: any) => form.examApplyIds.includes(apply.id))
      .reduce((sum: number, apply: any) => sum + Number(apply.amount || 0), 0)
)

function visitTypeName(type: number) {
  return { 1: '挂号费', 2: '门诊费', 3: '住院费' }[type] ?? '其他'
}

function payTypeName(type: number) {
  return { 1: '现金', 2: '微信', 3: '支付宝', 4: '医保', 5: '银行卡' }[type] ?? '其他'
}

function itemTypeName(type: number) {
  return { 1: '药品', 2: '诊疗', 3: '检查', 4: '床位', 5: '护理', 6: '挂号' }[type] ?? '其他'
}

function payStatusTag(status: number) {
  if (status === 0) return { label: '待支付', type: 'warning' as const }
  if (status === 1) return { label: '已支付', type: 'success' as const }
  return { label: '已退费', type: 'info' as const }
}

function addItem(id: number) {
  const fee = feeItemOptions.value.find((item) => item.id === id)
  if (!fee) return
  const exist = form.items.find((item) => item.itemId === fee.id)
  if (exist) {
    exist.quantity = Number(exist.quantity || 0) + 1
  } else {
    form.items.push({
      itemType: fee.itemType,
      itemId: fee.id,
      itemName: fee.itemName,
      quantity: 1,
      price: Number(fee.price || 0)
    })
  }
  pickerItemId.value = undefined
}

async function loadOptions() {
  const [feeItems] = await Promise.all([listFeeItems()])
  feeItemOptions.value = (feeItems || []) as any[]
  await loadPrescriptions()
}

/**
 * 加载与收费相关的处方。
 * <p>
 * 处方是医生开的医嘱，开立即生效、没有审核环节，所以「0 待收费」直接可收
 * （早期数据里的 1 已审核语义等同待收费，一并列出）；
 * 另外单独查一次 2 已收费，用于提示"不用重复收费"，不做静默过滤。
 */
async function loadPrescriptions(patientId?: any) {
  const hasPatient = patientId !== undefined && patientId !== null && patientId !== ''
  const base: any = { pageNum: 1, pageSize: 300 }
  if (hasPatient) {
    base.patientId = patientId
  }
  const [all, charged] = await Promise.all([
    pagePrescriptions(base),
    hasPatient ? pagePrescriptions({ ...base, status: 2 }) : Promise.resolve({ list: [] })
  ])
  const rows: any[] = (all as any)?.list || []
  prescriptionOptions.value = rows.filter((p: any) => p.status === 0 || p.status === 1)
  chargedPrescriptions.value = hasPatient ? ((charged as any)?.list || []) : []
}

/** 取患者最近一条挂号，用来带出科室（同一次就诊的科室才是这笔费用该归的科室）。 */
async function loadCurrentVisit(patientId: any) {
  currentVisit.value = null
  if (patientId === undefined || patientId === null || patientId === '') return
  const result: any = await pageRegistrations({ pageNum: 1, pageSize: 1, patientId })
  const row = result?.list?.[0]
  if (row && row.regTime && String(row.regTime).slice(0, 10) === localToday()) {
    currentVisit.value = row
  }
}

function localToday() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/**
 * 选择患者后：
 * 1. 清空上一笔的处方/明细/科室，避免把上一个患者的科室残留在表单里；
 * 2. 用该患者最近一次就诊的科室回填；
 * 3. 只列出该患者可收费的处方，并对"未审核通过"的处方给出明确提示。
 */
async function onPatientChange(patientId: any) {
  form.prescriptionId = undefined
  form.examApplyIds = []
  form.items = []
  form.deptId = undefined
  currentVisit.value = null
  chargedPrescriptions.value = []

  if (patientId === undefined || patientId === null || patientId === '') {
    prescriptionOptions.value = []
    examApplyOptions.value = []
    return
  }

  await Promise.all([loadPrescriptions(patientId), loadExamApplies(patientId), loadCurrentVisit(patientId)])

  if (currentVisit.value?.deptId !== undefined && currentVisit.value?.deptId !== null) {
    form.deptId = currentVisit.value.deptId
  }
}

/**
 * 加载待缴费（status=0）的检查申请单。
 * 收费时勾选后由后端自动生成"检查"明细，并把这单推进到待采样。
 */
async function loadExamApplies(patientId?: any) {
  if (patientId === undefined || patientId === null || patientId === '') {
    examApplyOptions.value = []
    return
  }
  const result: any = await pageExamApplies({ pageNum: 1, pageSize: 200, patientId, status: 0 })
  examApplyOptions.value = result?.list || []
}

/** 选择关联处方后，把处方药品同步进收费明细，并回填患者与科室。 */
async function onPrescriptionChange(prescriptionId: any) {
  form.items = []
  if (prescriptionId === undefined || prescriptionId === null || prescriptionId === '') {
    return
  }
  const result: any = await getPrescription(prescriptionId)
  const presc = result?.prescription
  const items: any[] = result?.items || []
  if (presc?.patientId !== undefined && presc?.patientId !== null) {
    form.patientId = presc.patientId
  }
  if (presc?.deptId !== undefined && presc?.deptId !== null) {
    form.deptId = presc.deptId
  }
  // 处方所属患者确定后，待缴费检查单跟着刷新，保证两者能一起结清
  form.examApplyIds = []
  await Promise.all([loadExamApplies(presc?.patientId), loadPrescriptions(presc?.patientId)])
  form.items = items.map((it) => ({
    itemType: 1,
    itemId: it.drugId,
    itemName: it.drugName,
    quantity: Number(it.quantity || 1),
    price: Number(it.price || 0)
  }))
  if (!form.items.length) {
    ElMessage.warning('该处方没有药品明细')
  }
}

function openDialog() {
  Object.assign(form, {
    patientId: undefined,
    deptId: undefined,
    prescriptionId: undefined,
    examApplyIds: [],
    visitType: 2,
    payType: 1,
    discountAmount: 0,
    items: []
  })
  examApplyOptions.value = []
  currentVisit.value = null
  chargedPrescriptions.value = []
  pickerItemId.value = undefined
  dialogVisible.value = true
  formRef.value?.clearValidate()
  // 恢复成"未选患者"时的处方候选，避免上一单的患者级过滤残留
  loadPrescriptions()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.items.length && !form.examApplyIds.length) {
    ElMessage.warning('请至少添加一条收费明细，或勾选一张待缴费的检查申请单')
    return
  }
  saving.value = true
  try {
    await createCharge({
      ...form,
      items: form.items.map((item) => ({ ...item })),
      examApplyIds: [...form.examApplyIds]
    })
    ElMessage.success('收费成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  const result: any = await chargeDetail(row.id)
  Object.assign(detail, result || {})
  detailVisible.value = true
}

function openRefund(row: any) {
  Object.assign(refundForm, {
    chargeId: row.id,
    chargeNo: row.chargeNo,
    amount: Number(row.actualAmount || 0),
    refundType: 2,
    reason: ''
  })
  refundVisible.value = true
  refundFormRef.value?.clearValidate()
}

async function submitRefund() {
  const valid = await refundFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await applyRefund(refundForm.chargeId, {
      amount: refundForm.amount,
      refundType: refundForm.refundType,
      reason: refundForm.reason
    })
    ElMessage.success('退费申请已提交')
    refundVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [patients, depts] = await Promise.all([loadPatientOptions(), loadDeptOptions()])
  patientOptions.value = patients
  deptOptions.value = depts
  await loadOptions()
  load()
  // 从检查申请页点"去收费"跳过来时，直接带上患者并列出待缴费的检查单
  const patientId = route.query.patientId
  if (patientId) {
    openDialog()
    form.patientId = patientId
    await onPatientChange(patientId)
  }
  // 从处方管理点"去收费"过来时，把处方也一并选中并铺开药品明细
  const prescriptionId = route.query.prescriptionId
  if (prescriptionId) {
    form.prescriptionId = prescriptionId
    await onPrescriptionChange(prescriptionId)
  }
})
</script>

<style scoped>
.item-picker {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.exam-apply-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.charge-total {
  padding: 12px 0 0;
  text-align: right;
  font-size: 15px;
  font-weight: 600;
  color: var(--hc-primary);
}

.charge-total .form-tip {
  margin-left: 16px;
  font-weight: 400;
}

.amount-strong {
  font-weight: 600;
  color: var(--hc-primary);
}

.field-tip {
  width: 100%;
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.field-tip--warn {
  color: var(--el-color-warning);
}

.presc-alert {
  margin-bottom: 14px;
}
</style>
