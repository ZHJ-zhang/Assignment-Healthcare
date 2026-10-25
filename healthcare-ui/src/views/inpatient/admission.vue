<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">住院登记</h2>
        <p class="page-header__desc">办理入院、转科与出院手续</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="住院号 / 患者姓名"
            clearable
            style="width: 210px"
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
            <el-option label="在院" :value="0" />
            <el-option label="已出院" :value="1" />
            <el-option label="已转科" :value="2" />
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
        <el-button v-perm="'inpatient:admission:add'" type="primary" :icon="Plus" @click="openDialog()">
          入院登记
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="admissionNo" label="住院号" min-width="140" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="科室" min-width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="床位" width="110">
            <template #default="{ row }">{{ bedLabel(row.bedId) }}</template>
          </el-table-column>
          <el-table-column label="主治医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="diagnosis" label="入院诊断" min-width="150" show-overflow-tooltip />
          <el-table-column label="医保类型" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('insurance_type', row.insuranceType) || '自费' }}</template>
          </el-table-column>
          <el-table-column label="押金" width="110" align="right">
            <template #default="{ row }">￥{{ Number(row.depositAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="admissionDate" label="入院日期" width="120" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'inpatient:admission:transfer'"
                link
                type="primary"
                :disabled="row.status === 1"
                @click="openTransfer(row)"
              >
                转科
              </el-button>
              <el-button
                v-perm="'inpatient:admission:discharge'"
                link
                type="danger"
                :disabled="row.status === 1"
                @click="discharge(row)"
              >
                出院
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

    <el-dialog v-model="dialogVisible" title="入院登记" width="620px">
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
            <el-form-item label="入院科室" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="床位" prop="bedId">
              <el-select v-model="form.bedId" placeholder="请选择空闲床位" style="width: 100%">
                <el-option
                  v-for="item in freeBeds"
                  :key="item.id"
                  :label="`${item.wardName} ${item.bedNo}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主治医生">
              <el-select v-model="form.doctorId" filterable placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in doctorOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入院日期">
              <el-date-picker
                v-model="form.admissionDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医保类型">
              <el-select v-model="form.insuranceType" style="width: 100%">
                <el-option label="自费" :value="0" />
                <el-option label="职工医保" :value="1" />
                <el-option label="居民医保" :value="2" />
                <el-option label="商业保险" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医保号">
              <el-input v-model="form.insuranceNo" placeholder="医保卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="住院押金">
              <el-input-number v-model="form.depositAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="入院诊断">
              <el-input v-model="form.diagnosis" type="textarea" :rows="2" placeholder="入院诊断" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认入院</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="患者转科" width="480px">
      <el-form label-width="92px">
        <el-form-item label="当前科室">{{ deptName(current.deptId) }}</el-form-item>
        <el-form-item label="转入科室" required>
          <el-select v-model="transferForm.deptId" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标床位">
          <el-select v-model="transferForm.bedId" placeholder="请选择空闲床位" clearable style="width: 100%">
            <el-option
              v-for="item in freeBeds"
              :key="item.id"
              :label="`${item.wardName} ${item.bedNo}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTransfer">确认转科</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createAdmission,
  dischargeAdmission,
  listBeds,
  pageAdmissions,
  pagePatients,
  transferAdmission
} from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const doctorOptions = ref<LookupOption[]>([])
const patientOptions = ref<any[]>([])
const beds = ref<any[]>([])
const route = useRoute()

const { list, total, loading, query, load, search, reset } = usePage(pageAdmissions, {
  keyword: '',
  deptId: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const transferVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const current = ref<any>({})

const form = reactive<any>({
  patientId: undefined,
  deptId: undefined,
  bedId: undefined,
  doctorId: undefined,
  admissionDate: new Date().toISOString().slice(0, 10),
  depositAmount: 1000,
  insuranceType: 0,
  insuranceNo: '',
  diagnosis: '',
  status: 0
})

const transferForm = reactive<any>({
  deptId: undefined,
  bedId: undefined
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择入院科室', trigger: 'change' }],
  bedId: [{ required: true, message: '请选择床位', trigger: 'change' }]
}

const freeBeds = computed(() => beds.value.filter((item) => item.status !== 1))

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function bedLabel(id: number) {
  const hit = beds.value.find((item) => item.id === id)
  return hit ? `${hit.wardName} ${hit.bedNo}` : '-'
}

function statusTag(value: number) {
  if (value === 0) return { label: '在院', type: 'success' as const }
  if (value === 1) return { label: '已出院', type: 'info' as const }
  return { label: '已转科', type: 'warning' as const }
}

async function loadBeds() {
  beds.value = ((await listBeds({})) as any[]) || []
}

function openDialog(prefill?: { patientId?: any; deptId?: any }) {
  Object.assign(form, {
    patientId: prefill?.patientId ?? undefined,
    deptId: prefill?.deptId ?? query.deptId,
    bedId: undefined,
    doctorId: undefined,
    admissionDate: new Date().toISOString().slice(0, 10),
    depositAmount: 1000,
    insuranceType: 0,
    insuranceNo: '',
    diagnosis: '',
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
    await createAdmission({ ...form })
    ElMessage.success('入院登记成功')
    dialogVisible.value = false
    load()
    loadBeds()
  } finally {
    saving.value = false
  }
}

function openTransfer(row: any) {
  current.value = row
  Object.assign(transferForm, { deptId: undefined, bedId: undefined })
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferForm.deptId) {
    ElMessage.warning('请选择转入科室')
    return
  }
  saving.value = true
  try {
    await transferAdmission(current.value.id, {
      deptId: transferForm.deptId,
      bedId: transferForm.bedId
    })
    ElMessage.success('转科成功')
    transferVisible.value = false
    load()
    loadBeds()
  } finally {
    saving.value = false
  }
}

async function discharge(row: any) {
  await ElMessageBox.confirm(`确认办理「${patientName(row.patientId)}」的出院手续吗？`, '提示', {
    type: 'warning'
  })
  await dischargeAdmission(row.id)
  ElMessage.success('出院办理成功')
  load()
  loadBeds()
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  doctorOptions.value = await loadDoctorOptions()
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  await useDicts(['insurance_type'])
  await loadBeds()
  load()
  // 从医生工作台「建议住院」按钮带过来的患者：自动弹出入院登记并预填患者/科室。
  // 按 id 归一把 query 里的字符串 id 对齐到 options 的原生类型，避免 el-select 匹配不上。
  if (route.query.patientId != null) {
    const qp = String(route.query.patientId)
    const hit = patientOptions.value.find((o: any) => String(o.id) === qp)
    openDialog({
      patientId: hit ? hit.id : route.query.patientId,
      deptId: route.query.deptId
    })
  }
})
</script>
