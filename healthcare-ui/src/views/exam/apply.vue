<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">检查申请</h2>
        <p class="page-header__desc">开具检验与检查申请单，跟踪采样与出报告进度</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="申请单号">
          <el-input
            v-model="query.keyword"
            placeholder="按申请单号搜索"
            clearable
            style="width: 190px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.examType" placeholder="全部" clearable style="width: 150px" @change="search()">
            <el-option label="检验" :value="1" />
            <el-option label="检查（影像）" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px" @change="search()">
            <el-option label="待缴费" :value="0" />
            <el-option label="待采样" :value="1" />
            <el-option label="检验中" :value="2" />
            <el-option label="已出报告" :value="3" />
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
        <el-button v-perm="'exam:apply:add'" type="primary" :icon="Plus" @click="openDialog()">
          开具申请单
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="applyNo" label="申请单号" width="170" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="申请医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="申请科室" width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.examType === 2 ? 'warning' : 'primary'" size="small">
                {{ row.examType === 2 ? '检查（影像）' : '检验' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="itemName" label="项目名称" min-width="150" show-overflow-tooltip />
          <el-table-column label="检查费" width="110" align="right">
            <template #default="{ row }">￥{{ Number(row.amount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="clinicalDiagnosis" label="临床诊断" min-width="160" show-overflow-tooltip />
          <el-table-column prop="applyTime" label="申请时间" width="170" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">
                {{ statusTag(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                link
                type="warning"
                @click="goCharge(row)"
              >
                去收费
              </el-button>
              <el-button
                v-perm="'exam:apply:sample'"
                link
                type="success"
                :disabled="row.status !== 1"
                @click="collect(row)"
              >
                采集样本
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

    <el-dialog v-model="dialogVisible" title="开具检查申请单" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者" prop="patientId">
              <el-select v-model="form.patientId" placeholder="请选择患者" filterable style="width: 100%">
                <el-option
                  v-for="item in patientOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请医生">
              <el-select
                v-model="form.doctorId"
                placeholder="默认当前用户"
                clearable
                filterable
                :disabled="deptLocked"
                style="width: 100%"
              >
                <el-option
                  v-for="item in doctorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请科室">
              <el-select
                v-model="form.deptId"
                placeholder="默认当前科室"
                clearable
                :disabled="deptLocked"
                style="width: 100%"
              >
                <el-option
                  v-for="item in deptOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查类型" prop="examType">
              <el-select v-model="form.examType" placeholder="请选择" style="width: 100%">
                <el-option label="检验" :value="1" />
                <el-option label="检查（影像）" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收费项目">
              <el-select
                v-model="pickedFeeItemId"
                placeholder="选择后自动带出名称与金额"
                clearable
                filterable
                style="width: 100%"
                @change="onFeeItemChange"
              >
                <el-option
                  v-for="item in examFeeItems"
                  :key="item.id"
                  :label="`${item.itemName} ￥${Number(item.price || 0).toFixed(2)}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="如 血常规" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查费">
              <el-input-number
                v-model="form.amount"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="临床诊断">
              <el-input
                v-model="form.clinicalDiagnosis"
                type="textarea"
                :rows="2"
                placeholder="临床诊断与检查目的"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { collectSample, createExamApply, listFeeItems, pageExamApplies } from '@/api/business'
import { usePage } from '@/composables/usePage'
import { useUserStore } from '@/store/modules/user'
import {
  deptName,
  doctorName,
  loadDeptOptions,
  loadDoctorOptions,
  loadPatientOptions,
  patientName,
  type LookupOption
} from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageExamApplies, {
  keyword: '',
  examType: undefined,
  status: undefined
})

const patientOptions = ref<LookupOption[]>([])
const doctorOptions = ref<LookupOption[]>([])
const deptOptions = ref<LookupOption[]>([])
const examFeeItems = ref<any[]>([])
const pickedFeeItemId = ref<number>()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
/** 医生的数据范围是"仅本人"：申请科室只能是自己的科室，不能替别的科室开检查。 */
const deptLocked = computed(() => userStore.isSelfScope && !!userStore.ownDeptId)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  patientId: undefined,
  doctorId: undefined,
  deptId: undefined,
  visitId: undefined,
  examType: 1,
  itemName: '',
  amount: 0,
  clinicalDiagnosis: ''
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  examType: [{ required: true, message: '请选择检查类型', trigger: 'change' }],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

function statusTag(value: number) {
  if (value === 0) return { label: '待缴费', type: 'warning' as const }
  if (value === 1) return { label: '待采样', type: 'primary' as const }
  if (value === 2) return { label: '检验中', type: 'primary' as const }
  return { label: '已出报告', type: 'success' as const }
}

function openDialog(prefill?: { patientId?: any; deptId?: any; visitId?: any }) {
  Object.assign(form, {
    patientId: prefill?.patientId ?? undefined,
    doctorId: deptLocked.value ? userStore.userInfo?.id : undefined,
    deptId: prefill?.deptId ?? (deptLocked.value ? userStore.ownDeptId : undefined),
    visitId: prefill?.visitId ?? undefined,
    examType: 1,
    itemName: '',
    amount: 0,
    clinicalDiagnosis: ''
  })
  pickedFeeItemId.value = undefined
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

/** 选中收费项目后自动带出项目名称与金额，避免手工填错价。 */
function onFeeItemChange(id: any) {
  const hit = examFeeItems.value.find((item) => String(item.id) === String(id))
  if (!hit) return
  form.itemName = hit.itemName
  form.amount = Number(hit.price || 0)
}

/** 待缴费的申请单直接引导到收费台，不再允许在这里手工"确认缴费"。 */
function goCharge(row: any) {
  router.push({ path: '/charge/index', query: { patientId: String(row.patientId) } })
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createExamApply({ ...form })
    ElMessage.success('申请单开具成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function collect(row: any) {
  await ElMessageBox.confirm(`确认为「${row.applyNo}」采集样本吗？`, '提示', { type: 'warning' })
  await collectSample(row.id)
  ElMessage.success('样本采集成功')
  load()
}

onMounted(async () => {
  const [patients, doctors, depts, feeItems] = await Promise.all([
    loadPatientOptions(),
    loadDoctorOptions(),
    loadDeptOptions(),
    listFeeItems({ itemType: 3 })
  ])
  patientOptions.value = patients
  doctorOptions.value = doctors
  deptOptions.value = depts
  examFeeItems.value = (feeItems || []) as any[]
  load()
  // 从医生工作台「开检查」按钮带过来的患者：自动弹出申请单并预填患者/科室/就诊，
  // 不必再手动在列表里翻找。选项已加载完，预填的患者名才能在下拉里正确显示。
  if (route.query.patientId) {
    openDialog({
      patientId: route.query.patientId,
      deptId: route.query.deptId,
      visitId: route.query.visitId
    })
  }
})
</script>
