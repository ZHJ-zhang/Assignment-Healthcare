<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">处方管理</h2>
        <p class="page-header__desc">
          开具、收费与作废门诊处方
          <span v-if="userStore.isSelfScope" class="page-header__scope">
            当前只显示 {{ userStore.userInfo?.deptName || '本人科室' }} 挂在你名下的患者
          </span>
        </p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="处方号"
            clearable
            style="width: 180px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="患者">
          <el-select v-model="query.patientId" filterable placeholder="全部" clearable style="width: 210px">
            <el-option
              v-for="item in patientOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待收费" :value="0" />
            <el-option label="已收费" :value="2" />
            <el-option label="已发药" :value="3" />
            <el-option label="已退" :value="4" />
            <el-option label="已作废" :value="5" />
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
        <el-button v-perm="'clinic:prescription:add'" type="primary" :icon="Plus" @click="openDialog()">
          开具处方
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="prescNo" label="处方号" min-width="140" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="开方医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" min-width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('presc_type', row.prescType) || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">明细</el-button>
              <el-button
                v-if="isUnpaid(row.status)"
                v-perm="'charge:add'"
                link
                type="success"
                @click="goCharge(row)"
              >
                去收费
              </el-button>
              <el-button
                v-perm="'clinic:prescription:remove'"
                link
                type="danger"
                :disabled="!isUnpaid(row.status)"
                @click="voidPrescription(row)"
              >
                作废
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

    <el-dialog v-model="dialogVisible" title="开具处方" width="880px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="患者" prop="patientId">
              <el-select v-model="form.patientId" filterable placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in patientOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="科室">
              <el-select
                v-model="form.deptId"
                placeholder="请选择"
                :disabled="deptLocked"
                style="width: 100%"
              >
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="处方类型">
              <el-select v-model="form.prescType" style="width: 100%">
                <el-option label="西药处方" :value="1" />
                <el-option label="中药处方" :value="2" />
                <el-option label="中成药处方" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="药品明细">
          <div class="presc-items">
            <div class="presc-items__head">
              <span>药品名称</span>
              <span>规格</span>
              <span>数量</span>
              <span>单价</span>
              <span>用法</span>
              <span>频次</span>
              <span />
            </div>
            <div v-for="(item, index) in form.items" :key="index" class="presc-items__row">
              <el-select v-model="item.drugId" filterable placeholder="选择药品" @change="onDrugChange(item)">
                <el-option
                  v-for="drug in drugOptions"
                  :key="drug.id"
                  :label="drug.drugName"
                  :value="drug.id"
                />
              </el-select>
              <el-input v-model="item.spec" placeholder="规格" />
              <el-input-number v-model="item.quantity" :min="1" :max="999" controls-position="right" />
              <el-input-number
                v-model="item.price"
                :min="0"
                :precision="2"
                :step="0.5"
                controls-position="right"
              />
              <el-input v-model="item.usage" placeholder="如 口服" />
              <el-input v-model="item.frequency" placeholder="如 每日三次" />
              <el-button link type="danger" @click="form.items.splice(index, 1)">移除</el-button>
            </div>
            <el-button link type="primary" :icon="Plus" @click="addItem">添加药品</el-button>
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="用药注意事项" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="presc-total">合计：￥{{ totalAmount.toFixed(2) }}</span>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存处方</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="处方明细" width="680px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="处方号">{{ detail.prescription?.prescNo }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ patientName(detail.prescription?.patientId) }}</el-descriptions-item>
        <el-descriptions-item label="开方医生">{{ doctorName(detail.prescription?.doctorId) }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ deptName(detail.prescription?.deptId) }}</el-descriptions-item>
        <el-descriptions-item label="合计金额">
          ￥{{ Number(detail.prescription?.totalAmount || 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.prescription?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.items || []" size="small" border style="margin-top: 14px">
        <el-table-column prop="drugName" label="药品名称" min-width="130" />
        <el-table-column prop="spec" label="规格" width="110" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">￥{{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="usage" label="用法" width="100" />
        <el-table-column prop="frequency" label="频次" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  createPrescription,
  getPrescription,
  pagePrescriptions,
  voidPrescription
} from '@/api/medical'
import { optionDrugs } from '@/api/option'
import { usePage } from '@/composables/usePage'
import { useUserStore } from '@/store/modules/user'
import { dictLabel, useDicts } from '@/utils/dict'
import {
  deptName,
  doctorName,
  loadDeptOptions,
  loadDoctorOptions,
  loadPatientOptions,
  patientName,
  type LookupOption
} from '@/utils/lookup'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/**
 * 医生的数据范围是"仅本人"：科室只能是自己的科室，不给下拉切换的机会。
 * 后端也认挂号记录上的科室为准，这里锁死只是让页面别撒谎。
 */
const deptLocked = computed(() => userStore.isSelfScope && !!userStore.ownDeptId)

/** 处方开立即生效，待收费（含历史"已审核"）都能直接带着患者和处方号跳到收费台。 */
function goCharge(row: any) {
  router.push({
    path: '/charge/index',
    query: { patientId: String(row.patientId), prescriptionId: String(row.id) }
  })
}

/** 0 待收费与历史 1 已审核都算"未收费"，可收费、可作废。 */
function isUnpaid(status: number) {
  return status === 0 || status === 1 || status === null || status === undefined
}
const patientOptions = ref<LookupOption[]>([])
const drugOptions = ref<any[]>([])
const deptOptions = ref<LookupOption[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pagePrescriptions, {
  keyword: '',
  patientId: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const detailVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const detail = ref<any>({})

const form = reactive<any>({
  patientId: undefined,
  visitId: undefined,
  deptId: undefined,
  prescType: 1,
  remark: '',
  items: [] as any[]
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }]
}

const totalAmount = computed(() =>
  form.items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
)

function statusTag(value: number) {
  if (value === 0) return { label: '待收费', type: 'warning' as const }
  if (value === 1) return { label: '待收费（历史已审核）', type: 'warning' as const }
  if (value === 2) return { label: '已收费', type: 'success' as const }
  if (value === 3) return { label: '已发药', type: 'primary' as const }
  if (value === 4) return { label: '已退', type: 'danger' as const }
  if (value === 5) return { label: '已作废', type: 'info' as const }
  return { label: '未知', type: 'info' as const }
}

function addItem() {
  form.items.push({
    drugId: undefined,
    drugName: '',
    spec: '',
    unit: '',
    quantity: 1,
    price: 0,
    usage: '口服',
    frequency: '每日三次'
  })
}

function onDrugChange(item: any) {
  const drug = drugOptions.value.find((item2) => item2.id === item.drugId)
  if (!drug) return
  item.drugName = drug.drugName
  item.spec = drug.spec
  item.unit = drug.unit
  item.price = Number(drug.salePrice || 0)
}

function openDialog() {
  // 注意：patientId / visitId / deptId 一律保留字符串，不能 Number()。
  // 后端 Long 走 ToStringSerializer，前端 option 的 value 也是字符串；
  // 一旦 Number() 会有两个坑：19 位雪花 ID 超过 Number.MAX_SAFE_INTEGER 精度溢出
  // （后端按失真 ID 查不到"就诊中"的挂号 → 报"该患者当前没有就诊中的挂号"），
  // 且 dept 的 select 的 value 是 string，被转成 number 后匹配不上 → 下拉直接显示"2"而非科室名。
  Object.assign(form, {
    patientId: route.query.patientId ? String(route.query.patientId) : undefined,
    visitId: route.query.visitId ? String(route.query.visitId) : undefined,
    // 锁死后一律用自己科室（本身是字符串），忽略地址栏里带来的 deptId
    deptId: deptLocked.value
      ? userStore.ownDeptId
      : route.query.deptId
        ? String(route.query.deptId)
        : undefined,
    prescType: 1,
    remark: '',
    items: []
  })
  addItem()
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.items.length || !form.items.some((item) => item.drugId)) {
    ElMessage.warning('请至少选择一种药品')
    return
  }
  saving.value = true
  try {
    await createPrescription({
      ...form,
      items: form.items.filter((item) => item.drugId)
    })
    ElMessage.success('处方保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function showDetail(row: any) {
  detail.value = (await getPrescription(row.id)) || {}
  detailVisible.value = true
}

async function voidPrescription(row: any) {
  await ElMessageBox.confirm(`确定作废处方「${row.prescNo}」吗？`, '提示', { type: 'warning' })
  await voidPrescription(row.id)
  ElMessage.success('已作废')
  load()
}

onMounted(async () => {
  // 下拉数据任一失败都不能拦住列表：之前是串行 await，药品接口一 403
  // 就直接把后面的 load() 跳过，页面表现成"无权限 + 暂无数据"。
  const [patients, drugs, depts] = await Promise.allSettled([
    loadPatientOptions(),
    optionDrugs(),
    loadDeptOptions()
  ])
  if (patients.status === 'fulfilled') patientOptions.value = patients.value
  if (drugs.status === 'fulfilled') drugOptions.value = drugs.value || []
  if (depts.status === 'fulfilled') deptOptions.value = depts.value
  // 医生姓名、处方类型只影响显示，失败也不影响查询
  await Promise.allSettled([loadDoctorOptions(), useDicts(['presc_type'])])
  await load()
})
</script>

<style scoped lang="scss">
.page-header__scope {
  margin-left: 8px;
  padding: 1px 8px;
  font-size: 12px;
  color: #0f8a7e;
  background: #e6f5f3;
  border-radius: 10px;
}

.presc-items {
  width: 100%;
}

.presc-items__head,
.presc-items__row {
  display: grid;
  grid-template-columns: 1.6fr 0.9fr 0.9fr 0.9fr 0.9fr 1fr 56px;
  gap: 6px;
  align-items: center;
}

.presc-items__head {
  padding: 6px 0;
  font-size: 12px;
  color: var(--hc-text-muted);
}

.presc-items__row {
  margin-bottom: 8px;
}

.presc-items__row :deep(.el-input-number) {
  width: 100%;
}

.presc-total {
  float: left;
  font-size: 15px;
  font-weight: 600;
  color: var(--hc-text);
  line-height: 32px;
}
</style>
