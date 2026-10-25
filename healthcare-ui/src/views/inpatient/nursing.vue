<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">护理记录</h2>
        <p class="page-header__desc">记录住院患者体温、脉搏等体征与护理交班</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="护理内容"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="患者">
          <el-select v-model="query.patientId" filterable placeholder="全部" clearable style="width: 210px">
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
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
        <el-button v-perm="'inpatient:nursing:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增护理记录
        </el-button>
        <el-button :icon="Document" @click="openCourse()">病程记录</el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }">{{ recordTypeLabel(row.recordType) }}</template>
          </el-table-column>
          <el-table-column label="体温" width="90" align="center">
            <template #default="{ row }">{{ row.temperature ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="血压" width="110" align="center">
            <template #default="{ row }">
              {{ row.systolicPressure ?? '-' }}/{{ row.diastolicPressure ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="心率" width="80" align="center">
            <template #default="{ row }">{{ row.heartRate ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="呼吸" width="80" align="center">
            <template #default="{ row }">{{ row.respiration ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="血氧" width="90" align="center">
            <template #default="{ row }">{{ row.bloodOxygen ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="content" label="护理内容" min-width="200" show-overflow-tooltip />
          <el-table-column label="科室" min-width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="recordTime" label="记录时间" min-width="170" />
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

    <el-dialog v-model="dialogVisible" title="新增护理记录" width="660px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者" prop="patientId">
              <el-select v-model="form.patientId" filterable placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in patientOptions"
                  :key="item.id"
                  :label="`${item.name}（${item.patientNo}）`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="住院记录">
              <el-select v-model="form.admissionId" filterable placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="item in admissions"
                  :key="item.id"
                  :label="`${item.admissionNo} ${patientName(item.patientId)}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录类型">
              <el-select v-model="form.recordType" style="width: 100%">
                <el-option label="体温单" :value="1" />
                <el-option label="生命体征" :value="2" />
                <el-option label="护理交班" :value="3" />
                <el-option label="特殊护理" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录科室">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体温" label-width="60px">
              <el-input-number v-model="form.temperature" :min="30" :max="45" :precision="1" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收缩压" label-width="60px">
              <el-input-number v-model="form.systolicPressure" :min="40" :max="260" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="舒张压" label-width="60px">
              <el-input-number v-model="form.diastolicPressure" :min="20" :max="160" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="心率" label-width="60px">
              <el-input-number v-model="form.heartRate" :min="20" :max="240" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="呼吸" label-width="60px">
              <el-input-number v-model="form.respiration" :min="5" :max="60" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血氧" label-width="60px">
              <el-input-number v-model="form.bloodOxygen" :min="50" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="护理内容">
              <el-input v-model="form.content" type="textarea" :rows="3" placeholder="护理措施与观察情况" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="courseVisible" title="病程记录" width="620px">
      <el-form label-width="92px">
        <el-form-item label="患者">
          <el-select v-model="courseForm.patientId" filterable placeholder="请选择" style="width: 100%">
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="courseForm.title" placeholder="如：首次病程记录" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="courseForm.content" type="textarea" :rows="6" placeholder="病程记录正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Document, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addCourseRecord,
  addNursingRecord,
  pageAdmissions,
  pageNursingRecords,
  pagePatients
} from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { deptName, loadDeptOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const patientOptions = ref<any[]>([])
const admissions = ref<any[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageNursingRecords, {
  keyword: '',
  admissionId: undefined,
  patientId: undefined
})

const dialogVisible = ref(false)
const courseVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  patientId: undefined,
  admissionId: undefined,
  deptId: undefined,
  recordType: 1,
  temperature: undefined,
  systolicPressure: undefined,
  diastolicPressure: undefined,
  heartRate: undefined,
  respiration: undefined,
  bloodOxygen: undefined,
  content: ''
})

const courseForm = reactive<any>({
  patientId: undefined,
  admissionId: undefined,
  deptId: undefined,
  recordType: 1,
  title: '',
  content: ''
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }]
}

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function recordTypeLabel(type: number) {
  const map: Record<number, string> = { 1: '体温单', 2: '生命体征', 3: '护理交班', 4: '特殊护理' }
  return map[type] || '-'
}

function openDialog() {
  Object.assign(form, {
    patientId: undefined,
    admissionId: undefined,
    deptId: undefined,
    recordType: 1,
    temperature: undefined,
    systolicPressure: undefined,
    diastolicPressure: undefined,
    heartRate: undefined,
    respiration: undefined,
    bloodOxygen: undefined,
    content: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await addNursingRecord({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openCourse() {
  Object.assign(courseForm, {
    patientId: undefined,
    admissionId: undefined,
    deptId: undefined,
    recordType: 1,
    title: '',
    content: ''
  })
  courseVisible.value = true
}

async function submitCourse() {
  if (!courseForm.patientId) {
    ElMessage.warning('请选择患者')
    return
  }
  saving.value = true
  try {
    await addCourseRecord({ ...courseForm })
    ElMessage.success('病程记录已保存')
    courseVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  const patients: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = patients?.list || []
  const admissionPage: any = await pageAdmissions({ pageNum: 1, pageSize: 200, status: 0 })
  admissions.value = admissionPage?.list || []
  load()
})
</script>
