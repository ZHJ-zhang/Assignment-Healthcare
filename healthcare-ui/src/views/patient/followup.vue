<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">随访管理</h2>
        <p class="page-header__desc">安排并跟踪出院患者的随访任务</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="随访内容"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="患者">
          <el-select
            v-model="query.patientId"
            filterable
            placeholder="全部"
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待随访" :value="0" />
            <el-option label="已随访" :value="1" />
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
        <el-button v-perm="'patient:followup:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增随访
        </el-button>
        <el-button :icon="Bell" @click="showPending">待办提醒</el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="patientId" label="患者" min-width="110">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column prop="planDate" label="计划随访日期" width="140" />
          <el-table-column label="随访方式" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('follow_type', row.followType) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="content" label="随访内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="result" label="随访结果" min-width="160" show-overflow-tooltip />
          <el-table-column label="科室" min-width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                {{ row.status === 1 ? '已随访' : '待随访' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'patient:followup:edit'" link type="primary" @click="openDialog(row)">
                修改
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改随访' : '新增随访'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者" prop="patientId">
              <el-select
                v-model="form.patientId"
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
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访日期" prop="planDate">
              <el-date-picker
                v-model="form.planDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访方式">
              <el-select v-model="form.followType" style="width: 100%">
                <el-option label="电话随访" :value="1" />
                <el-option label="上门随访" :value="2" />
                <el-option label="微信随访" :value="3" />
                <el-option label="门诊复诊" :value="4" />
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
            <el-form-item label="随访内容" prop="content">
              <el-input v-model="form.content" type="textarea" :rows="3" placeholder="随访要点" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="随访结果">
              <el-input v-model="form.result" type="textarea" :rows="3" placeholder="随访情况与处理建议" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">待随访</el-radio>
                <el-radio :value="1">已随访</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pendingVisible" title="待办随访提醒" width="680px">
      <el-table :data="pendingList" size="small" border max-height="420px">
        <el-table-column prop="planDate" label="计划日期" width="120" />
        <el-table-column prop="content" label="随访内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="deptId" label="科室" width="120">
          <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!pendingList.length" class="empty-hint">暂无待办随访</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Bell, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pageFollowUps, pagePatients, pendingFollowUps, saveFollowUp } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, loadDeptOptions, type LookupOption } from '@/utils/lookup'

const patientOptions = ref<any[]>([])
const deptOptions = ref<LookupOption[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageFollowUps, {
  keyword: '',
  patientId: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const pendingVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const pendingList = ref<any[]>([])

const form = reactive<any>({
  id: undefined,
  patientId: undefined,
  planDate: '',
  followType: 1,
  content: '',
  result: '',
  deptId: undefined,
  status: 0
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择随访日期', trigger: 'change' }],
  content: [{ required: true, message: '请输入随访内容', trigger: 'blur' }]
}

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function openDialog(row?: any) {
  Object.assign(form, row || {
    id: undefined,
    patientId: undefined,
    planDate: '',
    followType: 1,
    content: '',
    result: '',
    deptId: undefined,
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
    await saveFollowUp({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function showPending() {
  pendingList.value = ((await pendingFollowUps()) as any[]) || []
  pendingVisible.value = true
}

onMounted(async () => {
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  deptOptions.value = await loadDeptOptions()
  await useDicts(['follow_type'])
  load()
})
</script>
