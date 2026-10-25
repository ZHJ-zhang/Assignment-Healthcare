<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">预约管理</h2>
        <p class="page-header__desc">受理患者预约挂号、改约与取消</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="患者姓名"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待取号" :value="0" />
            <el-option label="已取号" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已爽约" :value="3" />
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
        <el-button v-perm="'appointment:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增预约
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="patientId" label="患者" min-width="110">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" min-width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="appointmentDate" label="预约日期" width="120" />
          <el-table-column label="时段" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('time_period', row.timePeriod) || '-' }}</template>
          </el-table-column>
          <el-table-column label="号别" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('reg_type', row.regType) || '-' }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110" align="center">
            <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cancelReason" label="取消原因" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.cancelReason || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-perm="'registration:add'"
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="checkin(row)"
              >
                取号
              </el-button>
              <el-button
                v-perm="'appointment:cancel'"
                link
                type="danger"
                :disabled="row.status !== 0"
                @click="cancel(row)"
              >
                取消
              </el-button>
              <el-button
                v-perm="'appointment:cancel'"
                link
                type="warning"
                :disabled="row.status !== 0"
                @click="noShow(row)"
              >
                爽约
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

    <el-dialog v-model="dialogVisible" title="新增预约" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="患者" prop="patientId">
          <el-select v-model="form.patientId" filterable placeholder="请选择患者" style="width: 100%">
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" prop="appointmentDate">
          <el-date-picker
            v-model="form.appointmentDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="form.doctorId" filterable placeholder="请选择医生" style="width: 100%">
            <el-option v-for="item in doctorOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择科室" style="width: 100%">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="form.timePeriod" style="width: 100%">
            <el-option label="上午" :value="1" />
            <el-option label="下午" :value="2" />
            <el-option label="夜间" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="号别">
          <el-select v-model="form.regType" style="width: 100%">
            <el-option label="普通号" :value="1" />
            <el-option label="专家号" :value="2" />
            <el-option label="急诊号" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约来源">
          <el-select v-model="form.source" style="width: 100%">
            <el-option label="窗口" :value="1" />
            <el-option label="自助机" :value="2" />
            <el-option label="线上预约" :value="3" />
            <el-option label="医生推荐" :value="4" />
          </el-select>
        </el-form-item>
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
import { cancelAppointment, checkinAppointment, createAppointment, noShowAppointment, pageAppointments, pagePatients } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions, type LookupOption } from '@/utils/lookup'

const patientOptions = ref<any[]>([])
const deptOptions = ref<LookupOption[]>([])
const doctorOptions = ref<LookupOption[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageAppointments, {
  keyword: '',
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  patientId: undefined,
  appointmentDate: '',
  doctorId: undefined,
  deptId: undefined,
  timePeriod: 1,
  regType: 1,
  source: 1,
  status: 0
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  appointmentDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }]
}

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function statusTag(status: number) {
  if (status === 0) return { label: '待取号', type: 'warning' as const }
  if (status === 1) return { label: '已取号', type: 'success' as const }
  if (status === 3) return { label: '已爽约', type: 'danger' as const }
  return { label: '已取消', type: 'info' as const }
}

function sourceLabel(source: number) {
  const map: Record<number, string> = { 1: '窗口', 2: '自助机', 3: '线上预约', 4: '医生推荐' }
  return map[source] || '窗口'
}

function openDialog() {
  Object.assign(form, {
    patientId: undefined,
    appointmentDate: new Date().toISOString().slice(0, 10),
    doctorId: undefined,
    deptId: undefined,
    timePeriod: 1,
    regType: 1,
    source: 1,
    status: 0
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createAppointment({ ...form })
    ElMessage.success('预约成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

/**
 * 取号：把线上预约转成正式挂号，进入候诊队列。
 * 用的是预约时已经占下的号源，不会再扣一次号；挂号费在这一步收取。
 */
async function checkin(row: any) {
  await ElMessageBox.confirm(
    `确认为「${patientName(row.patientId)}」取号吗？取号后生成挂号记录并进入 ${deptName(row.deptId) || ''} 候诊队列，` +
      '挂号费同时记入收费单。',
    '取号',
    { type: 'info' }
  )
  const reg: any = await checkinAppointment(row.id, 1)
  ElMessage.success(`取号成功，排队号 ${reg?.queueNo ?? '-'}`)
  load()
}

async function cancel(row: any) {
  const { value } = await ElMessageBox.prompt('请填写取消原因', '取消预约', {
    inputPlaceholder: '如：患者改期'
  })
  await cancelAppointment(row.id, value)
  ElMessage.success('已取消，号源已归还')
  load()
}

async function noShow(row: any) {
  await ElMessageBox.confirm(`确认将「${row.patientName || row.patientId}」的预约标记为爽约吗？号源将归还。`, '爽约登记', {
    type: 'warning'
  })
  await noShowAppointment(row.id)
  ElMessage.success('已登记爽约，号源已归还')
  load()
}

onMounted(async () => {
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  deptOptions.value = await loadDeptOptions()
  doctorOptions.value = await loadDoctorOptions()
  await useDicts(['time_period', 'reg_type'])
  load()
})
</script>
