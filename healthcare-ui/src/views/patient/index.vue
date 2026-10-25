<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">患者档案</h2>
        <p class="page-header__desc">管理患者基本信息，身份证号与手机号加密存储</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="姓名 / 病历号 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="query.gender" placeholder="全部" clearable style="width: 110px">
            <el-option label="男" :value="1" />
            <el-option label="女" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
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
        <el-button v-perm="'patient:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增患者
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="patientNo" label="病历号" min-width="120" />
          <el-table-column prop="name" label="姓名" min-width="100" />
          <el-table-column label="性别" width="70" align="center">
            <template #default="{ row }">{{ dictLabel('sys_gender', row.gender) }}</template>
          </el-table-column>
          <el-table-column prop="age" label="年龄" width="70" align="center">
            <template #default="{ row }">{{ row.age ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="idCardMasked" label="身份证号" min-width="170" />
          <el-table-column prop="phoneMasked" label="联系电话" min-width="130" />
          <el-table-column prop="bloodType" label="血型" width="80" align="center">
            <template #default="{ row }">{{ dictLabel('blood_type', row.bloodType) || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-perm="'patient:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button v-perm="'patient:remove'" link type="danger" @click="remove(row)">
                删除
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改患者' : '新增患者'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="患者姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="身份证号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="item in bloodTypes"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属科室">
              <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="item in deptOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="家庭住址">
              <el-input v-model="form.address" placeholder="详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="过敏史">
              <el-input v-model="form.allergyHistory" type="textarea" :rows="2" placeholder="药物 / 食物过敏情况" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="既往病史">
              <el-input v-model="form.medicalHistory" type="textarea" :rows="2" placeholder="既往疾病与手术史" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="家族史">
              <el-input v-model="form.familyHistory" type="textarea" :rows="2" placeholder="家族疾病史" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如 医保,慢病" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="患者档案" size="640px">
      <el-descriptions :column="2" border v-loading="detailLoading">
        <el-descriptions-item label="病历号">{{ detail.patientNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ dictLabel('sys_gender', detail.gender) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detail.age ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ detail.idCard }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detail.birthDate }}</el-descriptions-item>
        <el-descriptions-item label="血型">{{ dictLabel('blood_type', detail.bloodType) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="家庭住址" :span="2">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="过敏史" :span="2">{{ detail.allergyHistory || '无' }}</el-descriptions-item>
        <el-descriptions-item label="既往病史" :span="2">{{ detail.medicalHistory || '无' }}</el-descriptions-item>
        <el-descriptions-item label="家族史" :span="2">{{ detail.familyHistory || '无' }}</el-descriptions-item>
      </el-descriptions>

      <div class="content-card drawer-card">
        <div class="content-card__header">
          <h4 class="content-card__title">就诊卡</h4>
        </div>
        <div class="content-card__body content-card__body--flush">
          <el-table :data="summary.cards || []" size="small">
            <el-table-column prop="cardNo" label="卡号" min-width="130" />
            <el-table-column prop="balance" label="余额" width="100" align="right" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '正常' : '已挂失' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="issueTime" label="发卡时间" min-width="160" />
          </el-table>
        </div>
      </div>

      <div class="content-card drawer-card">
        <div class="content-card__header">
          <h4 class="content-card__title">就诊记录</h4>
        </div>
        <div class="content-card__body content-card__body--flush">
          <el-table :data="summary.records || []" size="small">
            <el-table-column prop="visitNo" label="就诊号" min-width="130" />
            <el-table-column prop="diagnosis" label="诊断" min-width="140" show-overflow-tooltip />
            <el-table-column prop="visitTime" label="就诊时间" min-width="160" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createPatient,
  deletePatient,
  getPatient,
  pagePatients,
  patientSummary,
  updatePatient
} from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, getDict, useDicts } from '@/utils/dict'
import { loadDeptOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const bloodTypes = ref<any[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pagePatients, {
  keyword: '',
  gender: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const detail = ref<any>({})
const summary = ref<any>({})

const form = reactive<any>({
  id: undefined,
  name: '',
  gender: 1,
  birthDate: '',
  idCard: '',
  phone: '',
  address: '',
  bloodType: '',
  allergyHistory: '',
  medicalHistory: '',
  familyHistory: '',
  tags: '',
  deptId: undefined,
  status: 1
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  idCard: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value && !/(^\d{15}$)|(^\d{17}([0-9Xx])$)/.test(value)) {
          callback(new Error('身份证号格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    gender: 1,
    birthDate: '',
    idCard: '',
    phone: '',
    address: '',
    bloodType: '',
    allergyHistory: '',
    medicalHistory: '',
    familyHistory: '',
    tags: '',
    deptId: undefined,
    status: 1
  })
}

async function openDialog(row?: any) {
  resetForm()
  if (row) {
    const detailData: any = await getPatient(row.id)
    Object.assign(form, detailData || {})
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
      await updatePatient(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createPatient({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function showDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = (await getPatient(row.id)) || {}
    summary.value = (await patientSummary(row.id)) || {}
  } finally {
    detailLoading.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除患者「${row.name}」的档案吗？`, '提示', { type: 'warning' })
  await deletePatient(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  await useDicts(['sys_gender', 'blood_type'])
  bloodTypes.value = getDict('blood_type')
  load()
})
</script>

<style scoped lang="scss">
.drawer-card {
  margin-top: 16px;
}

.drawer-card .content-card__title {
  font-size: 14px;
}
</style>
