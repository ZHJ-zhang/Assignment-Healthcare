<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">健康体检</h2>
        <p class="page-header__desc">体检套餐登记与结果评价管理</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="患者">
          <el-select
            v-model="query.patientId"
            placeholder="全部患者"
            clearable
            filterable
            style="width: 200px"
            @change="search()"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="待体检" :value="0" />
            <el-option label="已完成" :value="1" />
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
        <el-button v-perm="'health:checkup:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增体检记录
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="examNo" label="体检单号" width="170" />
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="packageName" label="体检套餐" min-width="170" show-overflow-tooltip />
          <el-table-column prop="examDate" label="体检日期" width="120" />
          <el-table-column prop="result" label="体检结果" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">{{ row.result || '-' }}</template>
          </el-table-column>
          <el-table-column prop="assessment" label="综合评价" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.assessment || '-' }}</template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="90" align="right" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                {{ row.status === 1 ? '已完成' : '待体检' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'health:checkup:add'" link type="primary" @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑体检记录' : '新增体检记录'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="体检人">
              <el-select v-model="form.patientId" placeholder="请选择患者" filterable clearable style="width: 100%">
                <el-option
                  v-for="item in patientOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                  @change="onPatientChange"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="体检人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体检套餐" prop="packageName">
              <el-input v-model="form.packageName" placeholder="如 基础体检套餐A" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体检日期">
              <el-date-picker
                v-model="form.examDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="体检结果">
              <el-input
                v-model="form.result"
                type="textarea"
                :rows="4"
                placeholder="各科室检查结果汇总"
              />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="综合评价">
              <el-input v-model="form.assessment" placeholder="健康评价建议" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评分">
              <el-input-number v-model="form.score" :min="0" :max="100" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">待体检</el-radio>
                <el-radio :value="1">已完成</el-radio>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createCheckup, pageCheckups } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { loadPatientOptions, type LookupOption } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageCheckups, {
  patientId: undefined,
  status: undefined
})

const patientOptions = ref<LookupOption[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  patientId: undefined,
  name: '',
  packageName: '',
  examDate: new Date().toISOString().slice(0, 10),
  result: '',
  assessment: '',
  score: 0,
  status: 1
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  packageName: [{ required: true, message: '请输入体检套餐', trigger: 'blur' }]
}

function onPatientChange(id: number) {
  const hit = patientOptions.value.find((item) => item.value === id)
  if (hit?.label) form.name = hit.label
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      patientId: row.patientId,
      name: row.name || '',
      packageName: row.packageName || '',
      examDate: row.examDate || '',
      result: row.result || '',
      assessment: row.assessment || '',
      score: row.score ?? 0,
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: undefined,
      patientId: undefined,
      name: '',
      packageName: '',
      examDate: new Date().toISOString().slice(0, 10),
      result: '',
      assessment: '',
      score: 0,
      status: 1
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
    await createCheckup({ ...form })
    ElMessage.success(form.id ? '修改成功' : '新增成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  patientOptions.value = await loadPatientOptions()
  load()
})
</script>
