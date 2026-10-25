<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">就诊卡管理</h2>
        <p class="page-header__desc">为患者发放、补办、挂失与注销就诊卡</p>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button v-perm="'patient:card:add'" type="primary" :icon="Plus" @click="openIssue()">
            发放就诊卡
          </el-button>
        </div>
        <div class="toolbar__right">
          <el-select
            v-model="currentPatientId"
            filterable
            placeholder="选择患者以查询卡片"
            style="width: 260px"
            @change="loadCards"
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
        <el-table v-loading="loading" :data="cards" border>
          <el-table-column prop="cardNo" label="卡号" min-width="140" />
          <el-table-column prop="patientId" label="患者ID" width="120" />
          <el-table-column label="余额" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.balance || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="卡类型" width="110" align="center">
            <template #default="{ row }">{{ row.cardType === 1 ? '储值卡' : '普通卡' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="cardStatusType(row.status)" size="small">
                {{ cardStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="issueTime" label="发卡时间" min-width="170" />
          <el-table-column prop="lossTime" label="挂失时间" min-width="170">
            <template #default="{ row }">{{ row.lossTime || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'patient:card:reissue'"
                link
                type="primary"
                :disabled="row.status !== 1"
                @click="openReissue(row)"
              >
                补办
              </el-button>
              <el-button
                v-perm="'patient:card:loss'"
                link
                type="danger"
                :disabled="row.status !== 1"
                @click="lose(row)"
              >
                挂失
              </el-button>
              <el-button
                v-perm="'patient:card:cancel'"
                link
                type="danger"
                :disabled="row.status === 2"
                @click="cancel(row)"
              >
                注销
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!currentPatientId" class="empty-hint">
          请先在上方选择患者，再查看其就诊卡
        </div>
      </div>
    </div>

    <el-dialog v-model="issueVisible" title="发放就诊卡" width="480px">
      <el-form ref="issueFormRef" :model="issueForm" :rules="issueRules" label-width="92px">
        <el-form-item label="患者" prop="patientId">
          <el-select
            v-model="issueForm.patientId"
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
        <el-form-item label="卡类型">
          <el-radio-group v-model="issueForm.cardType">
            <el-radio :value="1">储值卡</el-radio>
            <el-radio :value="2">普通卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="初始余额">
          <el-input-number v-model="issueForm.balance" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="issueForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitIssue">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reissueVisible" title="补办就诊卡" width="480px">
      <el-form label-width="92px">
        <el-form-item label="原卡号">{{ currentCard?.cardNo }}</el-form-item>
        <el-form-item label="补办说明">
          <el-input v-model="reissueRemark" type="textarea" :rows="3" placeholder="请填写补办原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reissueVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReissue">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  cancelPatientCard,
  issuePatientCard,
  listPatientCards,
  losePatientCard,
  pagePatients,
  reissuePatientCard
} from '@/api/medical'

const patientOptions = ref<any[]>([])
const cards = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const currentPatientId = ref<number>()

const issueVisible = ref(false)
const reissueVisible = ref(false)
const issueFormRef = ref<FormInstance>()
const currentCard = ref<any>()
const reissueRemark = ref('')

const issueForm = reactive<any>({
  patientId: undefined,
  cardType: 1,
  balance: 0,
  remark: ''
})

const issueRules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }]
}

async function loadPatients() {
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
}

async function loadCards() {
  if (!currentPatientId.value) {
    cards.value = []
    return
  }
  loading.value = true
  try {
    cards.value = ((await listPatientCards(currentPatientId.value)) as any[]) || []
  } finally {
    loading.value = false
  }
}

function openIssue() {
  Object.assign(issueForm, {
    patientId: currentPatientId.value,
    cardType: 1,
    balance: 0,
    remark: ''
  })
  issueVisible.value = true
  issueFormRef.value?.clearValidate()
}

async function submitIssue() {
  const valid = await issueFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await issuePatientCard({ ...issueForm })
    ElMessage.success('发卡成功')
    issueVisible.value = false
    currentPatientId.value = issueForm.patientId
    loadCards()
  } finally {
    saving.value = false
  }
}

function openReissue(row: any) {
  currentCard.value = row
  reissueRemark.value = ''
  reissueVisible.value = true
}

async function submitReissue() {
  if (!currentCard.value) return
  saving.value = true
  try {
    await reissuePatientCard({ id: currentCard.value.id, remark: reissueRemark.value })
    ElMessage.success('补办成功')
    reissueVisible.value = false
    loadCards()
  } finally {
    saving.value = false
  }
}

async function lose(row: any) {
  await ElMessageBox.confirm(`确定挂失卡号「${row.cardNo}」吗？`, '提示', { type: 'warning' })
  await losePatientCard(row.id)
  ElMessage.success('挂失成功')
  loadCards()
}

function cardStatusName(status: number) {
  if (status === 0) return '已挂失'
  if (status === 2) return '已注销'
  return '正常'
}

function cardStatusType(status: number) {
  if (status === 0) return 'warning'
  if (status === 2) return 'info'
  return 'success'
}

async function cancel(row: any) {
  const balance = Number(row.balance || 0)
  const tip =
    balance > 0
      ? `注销后卡号作废、不可恢复，卡内余额 ￥${balance.toFixed(2)} 一并退回并清零。`
      : '注销后卡号作废、不可恢复。'
  const { value } = await ElMessageBox.prompt(`确定注销卡号「${row.cardNo}」吗？${tip}请填写注销原因：`, '注销就诊卡', {
    type: 'warning',
    inputPlaceholder: '如：患者退卡离院',
    inputValidator: (v: string) => (v && v.trim() ? true : '注销原因必填')
  })
  await cancelPatientCard(row.id, value.trim())
  ElMessage.success('注销成功')
  loadCards()
}

onMounted(async () => {
  await loadPatients()
  if (patientOptions.value.length) {
    currentPatientId.value = patientOptions.value[0].id
    loadCards()
  }
})
</script>
