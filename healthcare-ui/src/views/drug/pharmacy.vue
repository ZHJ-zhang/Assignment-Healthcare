<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">药房发药</h2>
        <p class="page-header__desc">按处方收费状态发药与退药处理</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="处方号">
          <el-input
            v-model="query.keyword"
            placeholder="处方单号"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="待收费" :value="0" />
            <el-option label="已收费（待发药）" :value="2" />
            <el-option label="已发药" :value="3" />
            <el-option label="已退" :value="4" />
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
        <el-button v-perm="'drug:pharmacy:dispense'" type="primary" :icon="FirstAidKit" @click="openDispense()">
          处方发药
        </el-button>
        <el-button v-perm="'drug:pharmacy:return'" type="warning" :icon="RefreshLeft" @click="openReturn()">
          处方退药
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="prescNo" label="处方单号" width="170" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="开方医生" width="110">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="处方类型" width="110" align="center">
            <template #default="{ row }">{{ prescTypeName(row.prescType) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">￥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="审核时间" width="170">
            <template #default="{ row }">{{ row.auditTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">
                {{ statusTag(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewItems(row)">查看明细</el-button>
              <el-button
                v-perm="'drug:pharmacy:dispense'"
                link
                type="success"
                :disabled="row.status !== 2"
                @click="openDispense(row)"
              >
                发药
              </el-button>
              <el-button
                v-perm="'drug:pharmacy:return'"
                link
                type="warning"
                :disabled="row.status !== 3"
                @click="openReturn(row)"
              >
                退药
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

    <el-dialog
      v-model="actionVisible"
      :title="actionMode === 'dispense' ? '处方发药' : '处方退药'"
      width="760px"
    >
      <el-form ref="actionFormRef" :model="actionForm" :rules="actionRules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="处方单">
              <el-select
                v-model="actionForm.prescriptionId"
                placeholder="请选择处方"
                filterable
                style="width: 100%"
                @change="onPrescriptionChange"
              >
                <el-option
                  v-for="item in candidates"
                  :key="item.id"
                  :label="`${item.prescNo} · ${patientName(item.patientId) || '未知患者'}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发药库房" prop="warehouseId">
              <el-select v-model="actionForm.warehouseId" placeholder="请选择库房" style="width: 100%">
                <el-option
                  v-for="item in warehouseOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="actionMode === 'return'" :span="24">
            <el-form-item label="退药原因">
              <el-input v-model="actionForm.reason" type="textarea" :rows="2" placeholder="请填写退药原因" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table v-if="actionItems.length" :data="actionItems" border size="small">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="drugName" label="药品名称" min-width="140" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="usage" label="用法" min-width="120" />
        <el-table-column prop="frequency" label="频次" width="100" />
      </el-table>
      <p v-else class="empty-hint">请选择处方查看药品明细</p>

      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" @click="submitAction">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="处方明细" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="处方单号">{{ detail.prescNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ patientName(detail.patientId) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开方医生">{{ doctorName(detail.doctorId) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ deptName(detail.deptId) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额">￥{{ Number(detail.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(detail.status).type" size="small">{{ statusTag(detail.status).label }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailItems" border size="small" style="margin-top: 16px">
        <el-table-column prop="drugName" label="药品名称" min-width="140" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="price" label="单价" width="90" align="right" />
        <el-table-column prop="usage" label="用法" min-width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { FirstAidKit, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { dispense, listWarehouses, returnDrug } from '@/api/business'
import { getPrescription, pagePrescriptions } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions, loadPatientOptions, patientName } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pagePrescriptions, {
  keyword: '',
  status: undefined
})

const warehouseOptions = ref<any[]>([])

const actionVisible = ref(false)
const actionMode = ref<'dispense' | 'return'>('dispense')
const actionSaving = ref(false)
const actionFormRef = ref<FormInstance>()
const actionForm = reactive<any>({
  prescriptionId: undefined,
  warehouseId: undefined,
  reason: ''
})
const actionItems = ref<any[]>([])
const selectedItemIds = ref<number[]>([])

const detailVisible = ref(false)
const detail = reactive<any>({})
const detailItems = ref<any[]>([])

const actionRules: FormRules = {
  prescriptionId: [{ required: true, message: '请选择处方', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择库房', trigger: 'change' }]
}

const candidates = computed(() => {
  // 发药只能选「已收费」的处方，退药只能选「已发药」的处方，
  // 与后端 dispense/return 的状态校验保持一致，否则选出来也是必然失败的。
  const wanted = actionMode.value === 'dispense' ? 2 : 3
  return list.value.filter((item: any) => item.status === wanted)
})

function statusTag(value: number) {
  if (value === 0) return { label: '待收费', type: 'warning' as const }
  if (value === 1) return { label: '待收费（历史已审核）', type: 'warning' as const }
  if (value === 2) return { label: '待发药', type: 'success' as const }
  if (value === 3) return { label: '已发药', type: 'primary' as const }
  if (value === 4) return { label: '已退', type: 'danger' as const }
  if (value === 5) return { label: '已作废', type: 'info' as const }
  return { label: '未知', type: 'info' as const }
}

function prescTypeName(type: number) {
  return { 1: '西药处方', 2: '中药处方', 3: '中成药处方' }[type] ?? '其他'
}

async function loadWarehouses() {
  const rows: any[] = (await listWarehouses()) || []
  warehouseOptions.value = rows
    .filter((item) => item.status === 1)
    .map((item) => ({ value: item.id, label: item.warehouseName }))
}

async function loadPrescriptionItems(id: number) {
  const detailResult: any = await getPrescription(id)
  actionItems.value = detailResult?.items || []
  selectedItemIds.value = actionItems.value.map((item: any) => item.id)
}

async function onPrescriptionChange(id: number) {
  if (!id) {
    actionItems.value = []
    return
  }
  await loadPrescriptionItems(id)
}

async function openDispense(row?: any) {
  actionMode.value = 'dispense'
  Object.assign(actionForm, {
    prescriptionId: row?.id,
    warehouseId: warehouseOptions.value[0]?.value,
    reason: ''
  })
  actionItems.value = []
  selectedItemIds.value = []
  if (row?.id) await loadPrescriptionItems(row.id)
  actionVisible.value = true
  actionFormRef.value?.clearValidate()
}

async function openReturn(row?: any) {
  actionMode.value = 'return'
  Object.assign(actionForm, {
    prescriptionId: row?.id,
    warehouseId: warehouseOptions.value[0]?.value,
    reason: ''
  })
  actionItems.value = []
  selectedItemIds.value = []
  if (row?.id) await loadPrescriptionItems(row.id)
  actionVisible.value = true
  actionFormRef.value?.clearValidate()
}

async function submitAction() {
  const valid = await actionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  actionSaving.value = true
  try {
    const payload = {
      prescriptionId: actionForm.prescriptionId,
      warehouseId: actionForm.warehouseId,
      itemIds: selectedItemIds.value,
      reason: actionForm.reason
    }
    if (actionMode.value === 'dispense') {
      await dispense(payload)
      ElMessage.success('发药成功')
    } else {
      const res: any = await returnDrug(payload)
      const refundNo = res?.refundNo
      const refundAmount = res?.refundAmount
      if (refundNo) {
        ElMessage.success({
          message: `退药成功，已生成退费单 ${refundNo}（￥${refundAmount}），请到退费管理审核`,
          duration: 5000
        })
      } else {
        ElMessage.warning({
          message: '退药成功，药品已回库；该收费单已有处理中的退费单或无已支付收费单，未新开退费单，请到退费管理核对',
          duration: 6000
        })
      }
    }
    actionVisible.value = false
    load()
  } finally {
    actionSaving.value = false
  }
}

async function viewItems(row: any) {
  const detailResult: any = await getPrescription(row.id)
  Object.assign(detail, detailResult?.prescription || {})
  detailItems.value = detailResult?.items || []
  detailVisible.value = true
}

onMounted(async () => {
  await Promise.all([loadPatientOptions(), loadDoctorOptions(), loadDeptOptions(), loadWarehouses()])
  load()
})
</script>
