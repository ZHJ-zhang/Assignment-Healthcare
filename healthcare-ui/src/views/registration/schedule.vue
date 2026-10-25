<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">排班管理</h2>
        <p class="page-header__desc">维护医生出诊排班与号源数量</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="出诊日期">
          <el-date-picker
            v-model="query.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 160px"
            @change="load"
          />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 170px" @change="load">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生">
          <el-select v-model="query.doctorId" placeholder="全部" clearable style="width: 160px" @change="load">
            <el-option v-for="item in doctorOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-perm="'registration:schedule:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增排班
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="scheduleDate" label="出诊日期" width="120" />
          <el-table-column label="医生" width="120">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" min-width="140">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="时段" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('time_period', row.timePeriod) || '-' }}</template>
          </el-table-column>
          <el-table-column label="号别" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('reg_type', row.regType) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="totalNum" label="号源总数" width="100" align="center" />
          <el-table-column prop="remainNum" label="剩余号源" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="Number(row.remainNum) > 0 ? 'success' : 'danger'" size="small">
                {{ row.remainNum }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="挂号费" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.regFee || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '已停诊' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'registration:schedule:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button
                v-perm="'registration:schedule:stop'"
                link
                type="danger"
                :disabled="row.status !== 1"
                @click="stop(row)"
              >
                停诊
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改排班' : '新增排班'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出诊日期" prop="scheduleDate">
              <el-date-picker
                v-model="form.scheduleDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医生" prop="doctorId">
              <el-select v-model="form.doctorId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="item in doctorOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科室" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出诊时段">
              <el-select v-model="form.timePeriod" style="width: 100%">
                <el-option label="上午" :value="1" />
                <el-option label="下午" :value="2" />
                <el-option label="夜间" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="号别">
              <el-select v-model="form.regType" style="width: 100%">
                <el-option label="普通号" :value="1" />
                <el-option label="专家号" :value="2" />
                <el-option label="急诊号" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="号源总数" prop="totalNum">
              <el-input-number v-model="form.totalNum" :min="0" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="挂号费">
              <el-input-number v-model="form.regFee" :min="0" :precision="2" :step="5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停诊</el-radio>
              </el-radio-group>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listSchedules, saveSchedule, stopSchedule, updateSchedule } from '@/api/medical'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const doctorOptions = ref<LookupOption[]>([])
const list = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const query = reactive<any>({
  date: new Date().toISOString().slice(0, 10),
  deptId: undefined,
  doctorId: undefined
})

const form = reactive<any>({
  id: undefined,
  scheduleDate: '',
  doctorId: undefined,
  deptId: undefined,
  timePeriod: 1,
  regType: 1,
  totalNum: 30,
  remainNum: 30,
  regFee: 10,
  status: 1
})

const rules: FormRules = {
  scheduleDate: [{ required: true, message: '请选择出诊日期', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  totalNum: [{ required: true, message: '请输入号源总数', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    list.value = ((await listSchedules({ ...query })) as any[]) || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.date = new Date().toISOString().slice(0, 10)
  query.deptId = undefined
  query.doctorId = undefined
  load()
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      scheduleDate: query.date,
      doctorId: undefined,
      deptId: query.deptId,
      timePeriod: 1,
      regType: 1,
      totalNum: 30,
      remainNum: 30,
      regFee: 10,
      status: 1
    })
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateSchedule(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await saveSchedule({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function stop(row: any) {
  await ElMessageBox.confirm('确定将该排班设为停诊吗？', '提示', { type: 'warning' })
  await stopSchedule(row.id)
  ElMessage.success('已停诊')
  load()
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  doctorOptions.value = await loadDoctorOptions()
  await useDicts(['time_period', 'reg_type'])
  load()
})
</script>
