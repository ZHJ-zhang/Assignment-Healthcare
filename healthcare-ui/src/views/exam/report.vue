<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">检查报告</h2>
        <p class="page-header__desc">录入检验检查结果，完成审核与报告发布</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="报告标题 / 结论"
            clearable
            style="width: 210px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="草稿" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已发布" :value="2" />
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
        <el-button v-perm="'exam:report:add'" type="primary" :icon="Plus" @click="openDialog()">
          录入报告
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="reportTitle" label="报告标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="conclusion" label="结论" min-width="180" show-overflow-tooltip />
          <el-table-column label="危急值" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.criticalFlag === 1" type="danger" size="small">危急</el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="reportTime" label="报告时间" width="170" />
          <el-table-column prop="auditTime" label="审核时间" width="170">
            <template #default="{ row }">{{ row.auditTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">
                {{ statusTag(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
              <el-button
                v-perm="'exam:report:add'"
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="openDialog(row)"
              >
                修改
              </el-button>
              <el-button
                v-perm="'exam:report:audit'"
                link
                type="success"
                :disabled="row.status !== 0"
                @click="audit(row)"
              >
                审核
              </el-button>
              <el-button
                v-perm="'exam:report:publish'"
                link
                type="warning"
                :disabled="row.status !== 1"
                @click="publish(row)"
              >
                发布
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改报告' : '录入报告'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="关联申请" prop="applyId">
          <el-select
            v-model="form.applyId"
            placeholder="请选择申请单"
            filterable
            :disabled="!!form.id"
            style="width: 100%"
          >
            <el-option
              v-for="item in applyOptions"
              :key="item.id"
              :label="`${item.applyNo} · ${item.itemName || '未命名项目'}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报告标题" prop="reportTitle">
              <el-input v-model="form.reportTitle" placeholder="报告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="危急值">
              <el-radio-group v-model="form.criticalFlag">
                <el-radio :value="0">否</el-radio>
                <el-radio :value="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="检查结果" prop="reportContent">
              <el-input
                v-model="form.reportContent"
                type="textarea"
                :rows="4"
                placeholder="录入检验检查结果明细"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="结论" prop="conclusion">
              <el-input
                v-model="form.conclusion"
                type="textarea"
                :rows="2"
                placeholder="检查结论"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件地址">
              <el-input v-model="form.attachment" placeholder="影像或报告文件地址" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="报告详情" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报告标题">{{ detail.reportTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ patientName(detail.patientId) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报告时间">{{ detail.reportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detail.auditTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-block">
        <div class="detail-item__label">检查结果</div>
        <div class="detail-item__value detail-item__value--pre">{{ detail.reportContent || '-' }}</div>
      </div>
      <div class="detail-block">
        <div class="detail-item__label">结论</div>
        <div class="detail-item__value detail-item__value--pre">{{ detail.conclusion || '-' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  auditExamReport,
  getExamReport,
  pageExamApplies,
  pageExamReports,
  publishExamReport,
  saveExamReport
} from '@/api/business'
import { usePage } from '@/composables/usePage'
import { loadPatientOptions, patientName } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageExamReports, {
  keyword: '',
  status: undefined
})

const applyOptions = ref<any[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  applyId: undefined,
  reportTitle: '',
  reportContent: '',
  conclusion: '',
  criticalFlag: 0,
  attachment: ''
})

const rules: FormRules = {
  applyId: [{ required: true, message: '请选择申请单', trigger: 'change' }],
  reportTitle: [{ required: true, message: '请输入报告标题', trigger: 'blur' }],
  reportContent: [{ required: true, message: '请输入检查结果', trigger: 'blur' }],
  conclusion: [{ required: true, message: '请输入结论', trigger: 'blur' }]
}

const detailVisible = ref(false)
const detail = reactive<any>({})

function statusTag(value: number) {
  if (value === 0) return { label: '草稿', type: 'warning' as const }
  if (value === 1) return { label: '已审核', type: 'primary' as const }
  return { label: '已发布', type: 'success' as const }
}

async function loadApplies() {
  const result: any = await pageExamApplies({ pageNum: 1, pageSize: 300 })
  applyOptions.value = result?.list || []
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      applyId: row.applyId,
      reportTitle: row.reportTitle || '',
      reportContent: row.reportContent || '',
      conclusion: row.conclusion || '',
      criticalFlag: row.criticalFlag ?? 0,
      attachment: row.attachment || ''
    })
  } else {
    Object.assign(form, {
      id: undefined,
      applyId: undefined,
      reportTitle: '',
      reportContent: '',
      conclusion: '',
      criticalFlag: 0,
      attachment: ''
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
    await saveExamReport({ ...form })
    ElMessage.success(form.id ? '修改成功' : '录入成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function viewDetail(row: any) {
  const result: any = await getExamReport(row.id)
  Object.assign(detail, result || {})
  detailVisible.value = true
}

async function audit(row: any) {
  await ElMessageBox.confirm(`确认审核通过报告「${row.reportTitle}」吗？`, '提示', { type: 'warning' })
  await auditExamReport(row.id)
  ElMessage.success('审核成功')
  load()
}

async function publish(row: any) {
  await ElMessageBox.confirm(`确认发布报告「${row.reportTitle}」吗？`, '提示', { type: 'warning' })
  await publishExamReport(row.id)
  ElMessage.success('发布成功')
  load()
}

onMounted(async () => {
  await Promise.all([loadPatientOptions(), loadApplies()])
  load()
})
</script>

<style scoped>
.detail-block {
  margin-top: 14px;
}

.detail-item__value--pre {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
