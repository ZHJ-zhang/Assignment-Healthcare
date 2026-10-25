<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">预防接种</h2>
        <p class="page-header__desc">接种登记、剂次管理与下次接种提醒</p>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="接种记录" name="list">
          <el-form :model="query" inline class="search-bar">
            <el-form-item label="姓名">
              <el-input
                v-model="query.keyword"
                placeholder="接种人姓名"
                clearable
                style="width: 180px"
                @keyup.enter="search()"
                @clear="search()"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
                <el-option label="正常" :value="1" />
                <el-option label="取消" :value="0" />
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
            <el-button v-perm="'health:vaccination:add'" type="primary" :icon="Plus" @click="openDialog()">
              接种登记
            </el-button>
          </div>

          <div class="table-wrap">
            <el-table v-loading="loading" :data="list" border>
              <el-table-column prop="vaccinationNo" label="接种单号" width="170" />
              <el-table-column prop="name" label="姓名" width="110" />
              <el-table-column prop="vaccineName" label="疫苗名称" min-width="150" />
              <el-table-column prop="batchNo" label="疫苗批号" width="140" />
              <el-table-column prop="doseNo" label="剂次" width="80" align="center" />
              <el-table-column prop="vaccinationDate" label="接种日期" width="120" />
              <el-table-column prop="injectionSite" label="接种部位" width="110" />
              <el-table-column label="接种人" width="110">
                <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
              </el-table-column>
              <el-table-column prop="nextDate" label="下次接种" width="120">
                <template #default="{ row }">
                  <el-tag v-if="isDueSoon(row)" type="warning" size="small">{{ row.nextDate }}</el-tag>
                  <span v-else>{{ row.nextDate || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '正常' : '取消' }}
                  </el-tag>
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

        <el-tab-pane label="接种提醒" name="remind">
          <div class="table-wrap">
            <el-table v-loading="remindLoading" :data="remindList" border>
              <el-table-column prop="name" label="姓名" width="110" />
              <el-table-column prop="vaccineName" label="疫苗名称" min-width="150" />
              <el-table-column prop="doseNo" label="已完成剂次" width="110" align="center" />
              <el-table-column prop="nextDate" label="应种日期" width="130" />
              <el-table-column prop="injectionSite" label="接种部位" width="120" />
              <el-table-column label="状态" width="120" align="center">
                <template #default>
                  <el-tag type="warning" size="small">待接种</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="dialogVisible" title="接种登记" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联患者">
              <el-select v-model="form.patientId" placeholder="请选择患者" filterable clearable style="width: 100%">
                <el-option
                  v-for="item in patientOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                  @change="onPatientChange"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="接种人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="疫苗名称" prop="vaccineName">
              <el-input v-model="form.vaccineName" placeholder="如 乙肝疫苗" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="疫苗批号">
              <el-input v-model="form.batchNo" placeholder="批号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="剂次">
              <el-input-number v-model="form.doseNo" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="接种日期">
              <el-date-picker
                v-model="form.vaccinationDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="接种部位">
              <el-input v-model="form.injectionSite" placeholder="如 左上臂" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次接种">
              <el-date-picker
                v-model="form.nextDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接种人">
              <el-select v-model="form.doctorId" placeholder="默认当前用户" clearable filterable style="width: 100%">
                <el-option
                  v-for="item in doctorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
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
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createVaccination, pageVaccinations, vaccinationReminders } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { doctorName, loadDoctorOptions, loadPatientOptions, type LookupOption } from '@/utils/lookup'

const activeTab = ref('list')

const { list, total, loading, query, load, search, reset } = usePage(pageVaccinations, {
  keyword: '',
  status: undefined
})

const remindList = ref<any[]>([])
const remindLoading = ref(false)

const patientOptions = ref<LookupOption[]>([])
const doctorOptions = ref<LookupOption[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  patientId: undefined,
  name: '',
  vaccineName: '',
  batchNo: '',
  doseNo: 1,
  vaccinationDate: '',
  injectionSite: '',
  doctorId: undefined,
  nextDate: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  vaccineName: [{ required: true, message: '请输入疫苗名称', trigger: 'blur' }]
}

function isDueSoon(row: any) {
  if (!row.nextDate) return false
  const today = new Date().toISOString().slice(0, 10)
  const limit = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10)
  return row.nextDate >= today && row.nextDate <= limit
}

function onPatientChange(id: number) {
  const hit = patientOptions.value.find((item) => item.value === id)
  if (hit?.label) form.name = hit.label
}

async function loadReminders() {
  remindLoading.value = true
  try {
    const result: any = await vaccinationReminders({ pageNum: 1, pageSize: 200 })
    remindList.value = result?.list || []
  } finally {
    remindLoading.value = false
  }
}

function openDialog() {
  Object.assign(form, {
    patientId: undefined,
    name: '',
    vaccineName: '',
    batchNo: '',
    doseNo: 1,
    vaccinationDate: new Date().toISOString().slice(0, 10),
    injectionSite: '',
    doctorId: undefined,
    nextDate: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createVaccination({ ...form })
    ElMessage.success('接种登记成功')
    dialogVisible.value = false
    load()
    loadReminders()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [patients, doctors] = await Promise.all([loadPatientOptions(), loadDoctorOptions()])
  patientOptions.value = patients
  doctorOptions.value = doctors
  load()
})
</script>
