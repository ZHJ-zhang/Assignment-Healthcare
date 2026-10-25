<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">医生工作台</h2>
        <p class="page-header__desc">处理待诊患者，开具病历与处方</p>
      </div>
      <div class="toolbar__right">
        <span class="filter-label">科室下拉框</span>
        <el-select
          v-model="deptId"
          placeholder="全部科室"
          :disabled="deptLocked"
          style="width: 190px"
          @change="loadVisits"
        >
          <el-option label="全部" value="" />
          <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="status" placeholder="全部在诊" clearable style="width: 140px" @change="loadVisits">
          <el-option label="待就诊" :value="0" />
          <el-option label="就诊中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已退号" :value="3" />
        </el-select>
        <el-button :icon="Refresh" @click="loadVisits">刷新</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-card__icon" style="background: #fdf1e3; color: #d98324">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-card__body">
          <p class="stat-card__label">待就诊</p>
          <p class="stat-card__value">{{ countByStatus(0) }}<small>人</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: #e6f5f3; color: #0f8a7e">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-card__body">
          <p class="stat-card__label">就诊中</p>
          <p class="stat-card__value">{{ countByStatus(1) }}<small>人</small></p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: #fbe9ec; color: #c8394a">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="stat-card__body">
          <p class="stat-card__label">列表合计</p>
          <p class="stat-card__value">{{ visits.length }}<small>人</small></p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">就诊队列</h3>
      </div>
      <div class="table-wrap">
        <el-table v-loading="loading" :data="visits" border>
          <el-table-column prop="queueNo" label="排队号" width="90" align="center" />
          <el-table-column prop="regNo" label="挂号单号" min-width="140" />
          <el-table-column label="患者" min-width="110">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="号别" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('reg_type', row.regType) || '-' }}</template>
          </el-table-column>
          <el-table-column label="医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="regTime" label="挂号时间" min-width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="420" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'clinic:visit:receive'"
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="receive(row)"
              >
                接诊
              </el-button>
              <el-button
                v-perm="'clinic:record:save'"
                link
                type="primary"
                @click="openRecord(row)"
              >
                写病历
              </el-button>
              <el-button
                v-perm="'clinic:prescription:add'"
                link
                type="primary"
                @click="goPrescription(row)"
              >
                开处方
              </el-button>
              <el-button
                v-perm="'exam:apply:add'"
                link
                type="warning"
                @click="goExam(row)"
              >
                开检查
              </el-button>
              <el-button
                v-perm="'inpatient:admission:add'"
                link
                type="success"
                @click="goAdmission(row)"
              >
                建议住院
              </el-button>
              <el-button
                link
                type="success"
                :disabled="row.status !== 1"
                @click="finishVisit(row)"
              >
                完成就诊
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!visits.length" class="empty-hint">当前科室暂无挂号患者</div>
      </div>
    </div>

    <el-dialog v-model="recordVisible" title="书写门诊病历" width="720px">
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者">
              <el-input :model-value="recordPatientName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="就诊类型">
              <el-select v-model="recordForm.visitType" style="width: 100%">
                <el-option label="普通门诊" :value="1" />
                <el-option label="专家门诊" :value="2" />
                <el-option label="急诊" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主诉" prop="chiefComplaint">
              <el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" placeholder="患者主要症状" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现病史">
              <el-input v-model="recordForm.presentIllness" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="体格检查">
              <el-input v-model="recordForm.physicalExam" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="诊断" prop="diagnosis">
              <el-input v-model="recordForm.diagnosis" placeholder="临床诊断" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ICD 编码">
              <el-input v-model="recordForm.icdCode" placeholder="ICD-10 编码" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button :loading="saving" @click="saveRecord(false)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="saveRecord(true)">保存并提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { listVisits, receiveVisit, completeVisit, saveClinicRecord, submitClinicRecord, pageClinicRecords } from '@/api/medical'
import { useUserStore } from '@/store/modules/user'
import { dictLabel, useDicts } from '@/utils/dict'
import {
  doctorName,
  loadDeptOptions,
  loadDoctorOptions,
  loadPatientOptions,
  patientName,
  type LookupOption
} from '@/utils/lookup'

const router = useRouter()
const userStore = useUserStore()
/** 医生只能看自己科室、自己名下的号：科室筛选锁死，列表由后端按 doctor_id 收窄。 */
const deptLocked = computed(() => userStore.isSelfScope && !!userStore.ownDeptId)
const deptOptions = ref<LookupOption[]>([])
const visits = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
// 科室筛选：默认空字符串表示"全部科室"（不锁定单一科室，后端不过滤 deptId）。
// 用 string 而非 number，与 el-option 的 value（后端 Long 序列化成的字符串）保持一致，避免匹配不上。
const deptId = ref<string | null | undefined>('')
const status = ref<number>()

const recordVisible = ref(false)
const recordFormRef = ref<FormInstance>()
const currentVisit = ref<any>()

const recordForm = reactive<any>({
  id: undefined,
  patientId: undefined,
  deptId: undefined,
  visitType: 1,
  status: undefined,
  chiefComplaint: '',
  presentIllness: '',
  physicalExam: '',
  diagnosis: '',
  icdCode: ''
})

const recordRules: FormRules = {
  chiefComplaint: [{ required: true, message: '请输入主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断', trigger: 'blur' }]
}

const recordPatientName = computed(() => patientName(recordForm.patientId))

function statusTag(value: number) {
  if (value === 0) return { label: '待就诊', type: 'warning' as const }
  if (value === 1) return { label: '就诊中', type: 'primary' as const }
  if (value === 2) return { label: '已完成', type: 'success' as const }
  return { label: '已退号', type: 'info' as const }
}

function countByStatus(value: number) {
  return visits.value.filter((item) => item.status === value).length
}

async function loadVisits() {
  loading.value = true
  try {
    // deptId 为空字符串时代表"全部科室"，不往后端传，让后端按全部返回
    visits.value =
      ((await listVisits({
        ...(deptId.value ? { deptId: deptId.value } : {}),
        ...(status.value != null ? { status: status.value } : {})
      })) as any[]) || []
  } finally {
    loading.value = false
  }
}

async function receive(row: any) {
  await receiveVisit(row.id)
  ElMessage.success('接诊成功')
  loadVisits()
}

async function finishVisit(row: any) {
  await ElMessageBox.confirm(
    `确认为「${patientName(row.patientId)}」结束本次就诊吗？结束后该号不再出现在候诊队列，` +
      '未缴费的处方仍可在收费台继续结清。',
    '完成就诊',
    { type: 'warning' }
  )
  await completeVisit(row.id)
  ElMessage.success('本次就诊已完成')
  loadVisits()
}

async function openRecord(row: any) {
  currentVisit.value = row
  Object.assign(recordForm, {
    id: undefined,
    patientId: row.patientId,
    deptId: row.deptId,
    visitType: 1,
    status: undefined,
    chiefComplaint: '',
    presentIllness: '',
    physicalExam: '',
    diagnosis: '',
    icdCode: ''
  })
  recordVisible.value = true
  recordFormRef.value?.clearValidate()
  // 该次就诊若已有病历（草稿/已提交），拉回来回显：既不显示空白，也带着 id 走更新而不是重复插入
  try {
    const page: any = await pageClinicRecords({ visitId: row.id, pageNum: 1, pageSize: 1 })
    const existing = page?.list?.[0]
    if (existing) {
      Object.assign(recordForm, {
        id: existing.id,
        patientId: existing.patientId ?? row.patientId,
        deptId: existing.deptId ?? row.deptId,
        visitType: existing.visitType ?? 1,
        status: existing.status,
        chiefComplaint: existing.chiefComplaint ?? '',
        presentIllness: existing.presentIllness ?? '',
        physicalExam: existing.physicalExam ?? '',
        diagnosis: existing.diagnosis ?? '',
        icdCode: existing.icdCode ?? ''
      })
    }
  } catch {
    // 拉取失败不阻断书写，按新病历处理
  }
}

async function saveRecord(submit: boolean) {
  const valid = await recordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const saved: any = await saveClinicRecord({
      ...recordForm,
      visitNo: currentVisit.value?.regNo,
      visitId: currentVisit.value?.id
    })
    if (submit && saved?.id) {
      // 已提交过的病历再次提交后端会报"该病历已提交"，这里直接跳过重复提交
      if (saved.status === 1) {
        ElMessage.success('病历已提交，本次修改已保存')
      } else {
        await submitClinicRecord(saved.id)
        ElMessage.success('病历已提交')
      }
    } else {
      ElMessage.success('草稿已保存')
    }
    recordVisible.value = false
    loadVisits()
  } finally {
    saving.value = false
  }
}

function goPrescription(row: any) {
  router.push({
    path: '/clinic/prescription',
    query: { patientId: String(row.patientId), visitId: String(row.id), deptId: String(row.deptId) }
  })
}

function goExam(row: any) {
  router.push({
    path: '/exam/apply',
    query: { patientId: String(row.patientId), visitId: String(row.id), deptId: String(row.deptId) }
  })
}

function goAdmission(row: any) {
  router.push({
    path: '/inpatient/admission',
    query: { patientId: String(row.patientId), deptId: String(row.deptId) }
  })
}

onMounted(async () => {
  // 患者姓名走 /common/options/patients（登录即可访问，且医生的范围已被后端收窄到本人），
  // 不再依赖管理端的 /patient/page —— 医生没有那条权限，以前整列姓名都显示"-"。
  await Promise.allSettled([
    loadDeptOptions().then((rows) => {
      deptOptions.value = rows
    }),
    loadPatientOptions(),
    loadDoctorOptions(),
    useDicts(['reg_type'])
  ])
  if (deptLocked.value) {
    // 医生的号只会在自己科室里，直接把筛选落到自己的科室（下拉被禁用，无法切"全部"）
    deptId.value = userStore.ownDeptId
    loadVisits()
    return
  }
  // 非锁定视角（如超级管理员）：默认"全部科室"，再让用户自行下钻到具体科室
  deptId.value = ''
  loadVisits()
})
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  color: var(--el-text-color-regular, #606266);
  margin-right: 6px;
  white-space: nowrap;
}
</style>
