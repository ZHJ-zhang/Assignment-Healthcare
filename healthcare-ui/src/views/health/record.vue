<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">居民健康档案</h2>
        <p class="page-header__desc">建立居民电子健康档案，管理慢病与既往史</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="姓名">
          <el-input
            v-model="query.keyword"
            placeholder="居民姓名"
            clearable
            style="width: 180px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="社区">
          <el-input
            v-model="query.community"
            placeholder="所属社区"
            clearable
            style="width: 180px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="档案类型">
          <el-select v-model="query.recordType" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="一般档案" :value="1" />
            <el-option label="慢病档案" :value="2" />
            <el-option label="重点人群" :value="3" />
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
        <el-button v-perm="'health:record:add'" type="primary" :icon="Plus" @click="openDialog()">
          新建档案
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="recordNo" label="档案编号" width="160" />
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column label="性别" width="80" align="center">
            <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
          </el-table-column>
          <el-table-column prop="birthDate" label="出生日期" width="120" />
          <el-table-column label="身份证号" width="180">
            <template #default="{ row }">{{ row.idCard || '-' }}</template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column prop="community" label="所属社区" width="130" />
          <el-table-column label="档案类型" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="recordTypeTag(row.recordType).type">
                {{ recordTypeTag(row.recordType).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chronicType" label="慢病类型" width="120">
            <template #default="{ row }">{{ row.chronicType || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createDate" label="建档日期" width="120" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'health:record:edit'" link type="primary" @click="openDialog(row)">修改</el-button>
              <el-button v-perm="'health:record:edit'" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改健康档案' : '新建健康档案'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="居民姓名" />
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
            <el-form-item label="所属社区">
              <el-input v-model="form.community" placeholder="社区名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="档案类型" prop="recordType">
              <el-select v-model="form.recordType" placeholder="请选择" style="width: 100%">
                <el-option label="一般档案" :value="1" />
                <el-option label="慢病档案" :value="2" />
                <el-option label="重点人群" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="慢病类型">
              <el-input v-model="form.chronicType" placeholder="如 高血压" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="既往病史">
              <el-input
                v-model="form.medicalHistory"
                type="textarea"
                :rows="3"
                placeholder="既往疾病与手术史"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="家族史">
              <el-input
                v-model="form.familyHistory"
                type="textarea"
                :rows="2"
                placeholder="家族遗传病史"
              />
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
import { deleteHealthRecord, pageHealthRecords, saveHealthRecord, updateHealthRecord } from '@/api/medical'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageHealthRecords, {
  keyword: '',
  community: '',
  recordType: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  name: '',
  gender: 1,
  birthDate: '',
  idCard: '',
  phone: '',
  community: '',
  recordType: 1,
  chronicType: '',
  medicalHistory: '',
  familyHistory: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  recordType: [{ required: true, message: '请选择档案类型', trigger: 'change' }]
}

function recordTypeTag(type: number) {
  if (type === 1) return { label: '一般档案', type: 'primary' as const }
  if (type === 2) return { label: '慢病档案', type: 'warning' as const }
  return { label: '重点人群', type: 'danger' as const }
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, {
      ...row,
      birthDate: row.birthDate || '',
      idCard: row.idCard || '',
      phone: row.phone || '',
      community: row.community || '',
      chronicType: row.chronicType || '',
      medicalHistory: row.medicalHistory || '',
      familyHistory: row.familyHistory || ''
    })
  } else {
    Object.assign(form, {
      id: undefined,
      name: '',
      gender: 1,
      birthDate: '',
      idCard: '',
      phone: '',
      community: '',
      recordType: 1,
      chronicType: '',
      medicalHistory: '',
      familyHistory: ''
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
    if (form.id) {
      await updateHealthRecord(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await saveHealthRecord({ ...form })
      ElMessage.success('建档成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除档案「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteHealthRecord(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
