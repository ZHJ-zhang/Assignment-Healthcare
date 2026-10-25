<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">医嘱管理</h2>
        <p class="page-header__desc">开具、执行与停止住院医嘱</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="医嘱号 / 内容"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待执行" :value="0" />
            <el-option label="执行中" :value="1" />
            <el-option label="已停止" :value="2" />
            <el-option label="已完成" :value="3" />
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
        <el-button v-perm="'inpatient:order:add'" type="primary" :icon="Plus" @click="openDialog()">
          开具医嘱
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="orderNo" label="医嘱号" min-width="140" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('order_type', row.orderType) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="content" label="医嘱内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="dosage" label="剂量" width="110" />
          <el-table-column prop="frequency" label="频次" width="110" />
          <el-table-column label="科室" min-width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" min-width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'inpatient:order:execute'"
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="execute(row)"
              >
                执行
              </el-button>
              <el-button
                v-perm="'inpatient:order:execute'"
                link
                type="success"
                :disabled="row.status !== 1"
                @click="finish(row)"
              >
                完成
              </el-button>
              <el-button
                v-perm="'inpatient:order:stop'"
                link
                type="danger"
                :disabled="row.status === 2 || row.status === 3"
                @click="stop(row)"
              >
                停止
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

    <el-dialog v-model="dialogVisible" title="开具医嘱" width="620px">
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
            <el-form-item label="医嘱类型">
              <el-select v-model="form.orderType" style="width: 100%">
                <el-option label="长期医嘱" :value="1" />
                <el-option label="临时医嘱" :value="2" />
                <el-option label="护理医嘱" :value="3" />
                <el-option label="饮食医嘱" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行科室">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="医嘱内容" prop="content">
              <el-input v-model="form.content" type="textarea" :rows="2" placeholder="如：静脉滴注 0.9% 氯化钠注射液 250ml" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="剂量">
              <el-input v-model="form.dosage" placeholder="如 250ml" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="频次">
              <el-input v-model="form.frequency" placeholder="如 每日一次" />
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
import {
  createOrder,
  executeOrder,
  finishOrder,
  pageAdmissions,
  pageOrders,
  pagePatients,
  stopOrder
} from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, loadDeptOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const patientOptions = ref<any[]>([])
const admissions = ref<any[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageOrders, {
  keyword: '',
  admissionId: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  patientId: undefined,
  admissionId: undefined,
  deptId: undefined,
  doctorId: undefined,
  orderType: 1,
  content: '',
  dosage: '',
  frequency: '',
  status: 0
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  content: [{ required: true, message: '请输入医嘱内容', trigger: 'blur' }]
}

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function statusTag(value: number) {
  if (value === 0) return { label: '待执行', type: 'warning' as const }
  if (value === 1) return { label: '执行中', type: 'primary' as const }
  if (value === 2) return { label: '已停止', type: 'info' as const }
  if (value === 3) return { label: '已完成', type: 'success' as const }
  return { label: '未知', type: 'info' as const }
}

function openDialog() {
  Object.assign(form, {
    patientId: undefined,
    admissionId: undefined,
    deptId: undefined,
    doctorId: undefined,
    orderType: 1,
    content: '',
    dosage: '',
    frequency: '',
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
    await createOrder({ ...form })
    ElMessage.success('医嘱已开具')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function execute(row: any) {
  const { value } = await ElMessageBox.prompt('请填写执行结果（可留空）', '执行医嘱', {
    inputPlaceholder: '如：已给药，患者无不良反应'
  })
  await executeOrder(row.id, value)
  ElMessage.success('医嘱已执行')
  load()
}

async function finish(row: any) {
  await ElMessageBox.confirm(`确认医嘱「${row.orderNo}」已全部执行完毕吗？完成后不能再执行。`, '完成医嘱', {
    type: 'warning'
  })
  await finishOrder(row.id)
  ElMessage.success('医嘱已完成')
  load()
}

async function stop(row: any) {
  await ElMessageBox.confirm(`确定停止医嘱「${row.orderNo}」吗？`, '提示', { type: 'warning' })
  await stopOrder(row.id)
  ElMessage.success('医嘱已停止')
  load()
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  const patients: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = patients?.list || []
  const admissionPage: any = await pageAdmissions({ pageNum: 1, pageSize: 200, status: 0 })
  admissions.value = admissionPage?.list || []
  await useDicts(['order_type'])
  load()
})
</script>
