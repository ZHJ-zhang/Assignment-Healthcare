<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">传染病上报</h2>
        <p class="page-header__desc">法定传染病网络直报与病例登记</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="疾病名称">
          <el-input
            v-model="query.diseaseName"
            placeholder="按疾病名称搜索"
            clearable
            style="width: 190px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="待上报" :value="0" />
            <el-option label="已上报" :value="1" />
            <el-option label="已审核" :value="2" />
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
        <el-button v-perm="'health:infectious:add'" type="primary" :icon="Plus" @click="openDialog()">
          病例上报
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="reportNo" label="上报单号" width="160" />
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column label="性别" width="80" align="center">
            <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
          </el-table-column>
          <el-table-column prop="birthDate" label="出生日期" width="120" />
          <el-table-column prop="diseaseName" label="疾病名称" min-width="150" />
          <el-table-column prop="icdCode" label="ICD编码" width="130">
            <template #default="{ row }">{{ row.icdCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="onsetDate" label="发病日期" width="120" />
          <el-table-column prop="reportDate" label="报告日期" width="120" />
          <el-table-column prop="address" label="现住址" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核" min-width="170">
            <template #default="{ row }">
              <template v-if="row.status === 2">
                <div>{{ row.auditTime || '-' }}</div>
                <div class="audit-remark">{{ row.auditRemark || '-' }}</div>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                v-perm="'health:infectious:add'"
                link
                type="primary"
                @click="submitReport(row)"
              >
                提交上报
              </el-button>
              <el-button
                v-if="row.status === 1"
                v-perm="'health:infectious:audit'"
                link
                type="success"
                @click="audit(row)"
              >
                审核
              </el-button>
              <span v-if="row.status === 2" class="text-muted">已归档</span>
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

    <el-dialog v-model="dialogVisible" title="传染病病例上报" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="患者姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="疾病名称" prop="diseaseName">
              <el-input v-model="form.diseaseName" placeholder="如 肺结核" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ICD编码">
              <el-input v-model="form.icdCode" placeholder="ICD-10 编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发病日期">
              <el-date-picker
                v-model="form.onsetDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报告日期">
              <el-date-picker
                v-model="form.reportDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住址">
              <el-input v-model="form.address" placeholder="现住址" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存为待上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { auditInfectious, pageInfectious, reportInfectious, submitInfectious } from '@/api/medical'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageInfectious, {
  diseaseName: '',
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  name: '',
  gender: 1,
  birthDate: '',
  idCard: '',
  phone: '',
  address: '',
  diseaseName: '',
  icdCode: '',
  onsetDate: '',
  reportDate: new Date().toISOString().slice(0, 10)
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  diseaseName: [{ required: true, message: '请输入疾病名称', trigger: 'blur' }]
}

function openDialog() {
  Object.assign(form, {
    name: '',
    gender: 1,
    birthDate: '',
    idCard: '',
    phone: '',
    address: '',
    diseaseName: '',
    icdCode: '',
    onsetDate: '',
    reportDate: new Date().toISOString().slice(0, 10)
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await reportInfectious({ ...form })
    ElMessage.success('已保存为待上报')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function statusName(status: number) {
  if (status === 1) return '已上报'
  if (status === 2) return '已审核'
  return '待上报'
}

function statusType(status: number) {
  if (status === 1) return 'warning'
  if (status === 2) return 'success'
  return 'info'
}

async function submitReport(row: any) {
  await ElMessageBox.confirm(
    `确认把「${row.name} · ${row.diseaseName}」提交上报吗？提交后进入待审核。`,
    '提交上报',
    { type: 'warning' }
  )
  await submitInfectious(row.id)
  ElMessage.success('已提交上报')
  load()
}

async function audit(row: any) {
  const { value } = await ElMessageBox.prompt(
    `审核「${row.name} · ${row.diseaseName}」，确认后将锁定报卡。请填写审核意见：`,
    '报卡审核',
    {
      type: 'warning',
      inputPlaceholder: '如：信息完整，同意上报',
      inputValidator: (v: string) => (v && v.trim() ? true : '审核意见必填')
    }
  )
  await auditInfectious(row.id, value.trim())
  ElMessage.success('审核通过')
  load()
}

onMounted(load)
</script>

<style scoped lang="scss">
.audit-remark {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
