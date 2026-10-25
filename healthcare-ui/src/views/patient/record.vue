<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">病历管理</h2>
        <p class="page-header__desc">按患者归档门诊与住院病历</p>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button
            v-perm="'patient:record:list'"
            type="primary"
            :icon="Plus"
            :disabled="!currentPatientId"
            @click="openDialog()"
          >
            新增病历
          </el-button>
        </div>
        <div class="toolbar__right">
          <el-select
            v-model="currentPatientId"
            filterable
            placeholder="选择患者"
            style="width: 260px"
            @change="loadRecords"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
              :value="item.id"
            />
          </el-select>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="records" border>
          <el-table-column prop="visitNo" label="就诊号" min-width="130" />
          <el-table-column label="就诊类型" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('visit_type', row.visitType) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="chiefComplaint" label="主诉" min-width="160" show-overflow-tooltip />
          <el-table-column prop="diagnosis" label="诊断" min-width="140" show-overflow-tooltip />
          <el-table-column prop="icdCode" label="ICD 编码" width="120" />
          <el-table-column label="科室" min-width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="医生" width="100">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="visitTime" label="就诊时间" min-width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                {{ row.status === 1 ? '已完成' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!currentPatientId" class="empty-hint">请先选择患者</div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增病历" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者">
              <el-input :model-value="currentPatientLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="就诊类型">
              <el-select v-model="form.visitType" style="width: 100%">
                <el-option label="普通门诊" :value="1" />
                <el-option label="专家门诊" :value="2" />
                <el-option label="急诊" :value="3" />
                <el-option label="住院" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主诉" prop="chiefComplaint">
              <el-input v-model="form.chiefComplaint" type="textarea" :rows="2" placeholder="患者主要症状" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现病史">
              <el-input v-model="form.presentIllness" type="textarea" :rows="3" placeholder="疾病发生发展情况" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="体格检查">
              <el-input v-model="form.physicalExam" type="textarea" :rows="2" placeholder="查体所见" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="诊断" prop="diagnosis">
              <el-input v-model="form.diagnosis" placeholder="临床诊断" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ICD 编码">
              <el-input v-model="form.icdCode" placeholder="ICD-10 编码" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="病历详情" size="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="就诊号">{{ current.visitNo }}</el-descriptions-item>
        <el-descriptions-item label="主诉">{{ current.chiefComplaint || '无' }}</el-descriptions-item>
        <el-descriptions-item label="现病史">{{ current.presentIllness || '无' }}</el-descriptions-item>
        <el-descriptions-item label="体格检查">{{ current.physicalExam || '无' }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ current.diagnosis || '无' }}</el-descriptions-item>
        <el-descriptions-item label="ICD 编码">{{ current.icdCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">{{ current.visitTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pagePatients, patientRecords, savePatientRecord } from '@/api/medical'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions } from '@/utils/lookup'

const patientOptions = ref<any[]>([])
const records = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const currentPatientId = ref<number>()
const dialogVisible = ref(false)
const detailVisible = ref(false)
const current = ref<any>({})
const formRef = ref<FormInstance>()

const form = reactive<any>({
  visitType: 1,
  chiefComplaint: '',
  presentIllness: '',
  physicalExam: '',
  diagnosis: '',
  icdCode: ''
})

const rules: FormRules = {
  chiefComplaint: [{ required: true, message: '请输入主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断', trigger: 'blur' }]
}

const currentPatientLabel = computed(() => {
  const hit = patientOptions.value.find((item) => item.id === currentPatientId.value)
  return hit ? `${hit.name}（${hit.patientNo}）` : ''
})

async function loadRecords() {
  if (!currentPatientId.value) {
    records.value = []
    return
  }
  loading.value = true
  try {
    records.value = ((await patientRecords(currentPatientId.value)) as any[]) || []
  } finally {
    loading.value = false
  }
}

function openDialog() {
  Object.assign(form, {
    visitType: 1,
    chiefComplaint: '',
    presentIllness: '',
    physicalExam: '',
    diagnosis: '',
    icdCode: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await savePatientRecord({ ...form, patientId: currentPatientId.value })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadRecords()
  } finally {
    saving.value = false
  }
}

function showDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

onMounted(async () => {
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  await Promise.all([loadDeptOptions(), loadDoctorOptions()])
  await useDicts(['visit_type'])
})
</script>
