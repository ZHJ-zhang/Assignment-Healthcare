<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">出院结算</h2>
        <p class="page-header__desc">办理住院费用结算与医保对账</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="住院号">
          <el-select v-model="query.admissionId" filterable placeholder="全部" clearable style="width: 240px">
            <el-option
              v-for="item in admissions"
              :key="item.id"
              :label="`${item.admissionNo} ${patientName(item.patientId)}`"
              :value="item.id"
            />
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
        <el-button v-perm="'inpatient:settlement:list'" type="primary" :icon="Money" @click="openSettle()">
          出院结算
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="settlementNo" label="结算单号" min-width="140" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="科室" min-width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="自费金额" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.selfAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="医保金额" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.insuranceAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="押金" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.depositAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="应退金额" width="120" align="right">
            <template #default="{ row }">
              <span :class="Number(row.refundAmount) >= 0 ? 'refund-positive' : 'refund-negative'">
                ￥{{ Number(row.refundAmount || 0).toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="settleTime" label="结算时间" min-width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="settleStatusType(row.status)" size="small">
                {{ settleStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="退费" min-width="180">
            <template #default="{ row }">
              <template v-if="row.status === 2">
                <div>{{ row.refundTime || '-' }}</div>
                <div class="refund-reason">{{ row.refundReason || '-' }}</div>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1"
                v-perm="'inpatient:settlement:list'"
                link
                type="danger"
                @click="onRefund(row)"
              >
                退费
              </el-button>
              <span v-else>-</span>
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

    <el-dialog v-model="settleVisible" title="出院结算" width="560px">
      <el-form ref="settleFormRef" :model="settleForm" :rules="settleRules" label-width="100px">
        <el-form-item label="住院记录" prop="admissionId">
          <el-select
            v-model="settleForm.admissionId"
            filterable
            placeholder="请选择住院记录"
            style="width: 100%"
          >
            <el-option
              v-for="item in admissions"
              :key="item.id"
              :label="`${item.admissionNo} ${patientName(item.patientId)}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="住院押金">
          <el-input :model-value="`￥${Number(currentDeposit).toFixed(2)}`" disabled />
        </el-form-item>
        <el-form-item label="医保支付" prop="insuranceAmount">
          <el-input-number v-model="settleForm.insuranceAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="个人自付" prop="selfAmount">
          <el-input-number v-model="settleForm.selfAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="应退押金">
          <el-input :model-value="`￥${refundPreview.toFixed(2)}`" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSettle">确认结算</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Money, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { pageAdmissions, pagePatients, pageSettlements, refundSettlement, settleAdmission } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { deptName, loadDeptOptions } from '@/utils/lookup'

const patientOptions = ref<any[]>([])
const admissions = ref<any[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageSettlements, {
  admissionId: undefined
})

const settleVisible = ref(false)
const saving = ref(false)
const settleFormRef = ref<FormInstance>()

const settleForm = reactive<any>({
  admissionId: undefined,
  insuranceAmount: 0,
  selfAmount: 0
})

const settleRules: FormRules = {
  admissionId: [{ required: true, message: '请选择住院记录', trigger: 'change' }]
}

const currentDeposit = computed(() => {
  const hit = admissions.value.find((item) => item.id === settleForm.admissionId)
  return Number(hit?.depositAmount || 0)
})

const refundPreview = computed(() => currentDeposit.value - Number(settleForm.selfAmount || 0))

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function settleStatusName(status: number) {
  if (status === 1) return '已结算'
  if (status === 2) return '已退费'
  return '待结算'
}

function settleStatusType(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

async function onRefund(row: any) {
  const { value } = await ElMessageBox.prompt(
    `结算单 ${row.settlementNo} 退费后状态转为「已退费」，且不可再改回。请填写退费原因：`,
    '结算退费',
    {
      type: 'warning',
      inputPlaceholder: '如：患者要求转院、重复收费',
      inputValidator: (v: string) => (v && v.trim() ? true : '退费原因必填')
    }
  )
  await refundSettlement(row.id, value.trim())
  ElMessage.success('退费完成')
  load()
}

function openSettle() {
  Object.assign(settleForm, { admissionId: undefined, insuranceAmount: 0, selfAmount: 0 })
  settleVisible.value = true
  settleFormRef.value?.clearValidate()
}

async function submitSettle() {
  const valid = await settleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await settleAdmission({
      admissionId: settleForm.admissionId,
      insuranceAmount: settleForm.insuranceAmount,
      selfAmount: settleForm.selfAmount
    })
    ElMessage.success('结算成功')
    settleVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadDeptOptions()
  const patients: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = patients?.list || []
  const admissionPage: any = await pageAdmissions({ pageNum: 1, pageSize: 200 })
  admissions.value = admissionPage?.list || []
  load()
})
</script>

<style scoped lang="scss">
.refund-positive {
  color: var(--hc-success);
  font-weight: 600;
}

.refund-negative {
  color: var(--hc-danger);
  font-weight: 600;
}

.refund-reason {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
