<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">挂号管理</h2>
        <p class="page-header__desc">受理患者挂号、退号与就诊状态流转</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="挂号单号 / 患者姓名"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待就诊" :value="0" />
            <el-option label="就诊中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已退号" :value="3" />
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
        <el-button v-perm="'registration:add'" type="primary" :icon="Plus" @click="openRegister()">
          办理挂号
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="regNo" label="挂号单号" min-width="140" />
          <el-table-column label="患者" min-width="110">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" min-width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="号别" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('reg_type', row.regType) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="queueNo" label="排队号" width="90" align="center" />
          <el-table-column label="挂号费" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.regFee || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="regTime" label="挂号时间" min-width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'registration:call'"
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="call(row)"
              >
                叫号
              </el-button>
              <el-button
                v-perm="'clinic:visit:receive'"
                link
                type="primary"
                :disabled="row.status !== 1"
                @click="complete(row)"
              >
                完成就诊
              </el-button>
              <el-button
                v-perm="'registration:cancel'"
                link
                type="danger"
                :disabled="row.status !== 0"
                @click="cancel(row)"
              >
                退号
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load()"
          @size-change="search()"
        />
      </div>
    </div>

    <el-dialog v-model="registerVisible" title="办理挂号" width="600px">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="92px">
        <el-form-item label="患者" prop="patientId">
          <el-select
            v-model="registerForm.patientId"
            filterable
            placeholder="请选择患者"
            style="width: 100%"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出诊排班" prop="scheduleId">
          <el-select
            v-model="registerForm.scheduleId"
            placeholder="请选择排班"
            style="width: 100%"
            @change="onScheduleChange"
          >
            <el-option
              v-for="item in schedules"
              :key="item.id"
              :label="`${item.scheduleDate} ${doctorName(item.doctorId) || ''} ${dictLabel('time_period', item.timePeriod) || ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="号别">
          <el-select v-model="registerForm.regType" style="width: 100%">
            <el-option label="普通号" :value="1" />
            <el-option label="专家号" :value="2" />
            <el-option label="急诊号" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="挂号费">
          <el-input :model-value="`￥${Number(selectedFee).toFixed(2)}`" disabled />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="registerForm.payType" style="width: 100%">
            <el-option label="现金" :value="1" />
            <el-option label="微信" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="医保" :value="4" />
            <el-option label="银行卡" :value="5" />
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="确认挂号时挂号费会自动记入收费单（已支付），退号时自动退回，无需再去收费台重收一次。"
        />
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRegister">确认挂号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  cancelRegistration,
  callNumber,
  completeVisit,
  listSchedules,
  pagePatients,
  pageRegistrations,
  register
} from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions } from '@/utils/lookup'

const patientOptions = ref<any[]>([])
const schedules = ref<any[]>([])
const deptOptions = ref<any[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageRegistrations, {
  keyword: '',
  deptId: undefined,
  status: undefined
})

const registerVisible = ref(false)
const saving = ref(false)
const registerFormRef = ref<FormInstance>()

const registerForm = reactive<any>({
  patientId: undefined,
  scheduleId: undefined,
  regType: 1,
  payType: 1
})

const registerRules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  scheduleId: [{ required: true, message: '请选择排班', trigger: 'change' }]
}

const selectedFee = computed(() => {
  const hit = schedules.value.find((item) => item.id === registerForm.scheduleId)
  return Number(hit?.regFee || 0)
})

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function statusTag(status: number) {
  if (status === 0) return { label: '待就诊', type: 'warning' as const }
  if (status === 1) return { label: '就诊中', type: 'primary' as const }
  if (status === 2) return { label: '已完成', type: 'success' as const }
  return { label: '已退号', type: 'info' as const }
}

async function loadSchedules() {
  schedules.value =
    ((await listSchedules({ date: new Date().toISOString().slice(0, 10) })) as any[]) || []
}

function onScheduleChange() {
  const hit = schedules.value.find((item) => item.id === registerForm.scheduleId)
  if (hit?.regType) registerForm.regType = hit.regType
}

async function openRegister() {
  Object.assign(registerForm, { patientId: undefined, scheduleId: undefined, regType: 1, payType: 1 })
  await loadSchedules()
  registerVisible.value = true
  registerFormRef.value?.clearValidate()
}

async function submitRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await register({
      patientId: registerForm.patientId,
      scheduleId: registerForm.scheduleId,
      regType: registerForm.regType,
      payType: registerForm.payType
    })
    ElMessage.success('挂号成功，挂号费已记入收费单')
    registerVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function call(row: any) {
  await callNumber(row.id)
  ElMessage.success(`已叫号：${row.queueNo ?? '-'}`)
  load()
}

async function complete(row: any) {
  await ElMessageBox.confirm('确认该患者已完成本次就诊吗？', '提示', { type: 'warning' })
  await completeVisit(row.id)
  ElMessage.success('操作成功')
  load()
}

async function cancel(row: any) {
  const { value } = await ElMessageBox.prompt('请填写退号原因', '退号', {
    inputPlaceholder: '如：患者临时有事'
  })
  await cancelRegistration(row.id, value)
  ElMessage.success('退号成功')
  load()
}

onMounted(async () => {
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  const [, depts] = await Promise.all([loadDoctorOptions(), loadDeptOptions()])
  deptOptions.value = depts
  await useDicts(['reg_type', 'time_period'])
  load()
})
</script>
