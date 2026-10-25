<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">床位管理</h2>
        <p class="page-header__desc">维护病区床位配置与占用状态</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 180px" @change="load">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px" @change="load">
            <el-option label="空闲" :value="0" />
            <el-option label="占用" :value="1" />
            <el-option label="维修" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-perm="'inpatient:bed:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增床位
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="wardName" label="病区" min-width="140" />
          <el-table-column prop="bedNo" label="床号" width="110" />
          <el-table-column label="所属科室" min-width="140">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="床位类型" width="120" align="center">
            <template #default="{ row }">{{ dictLabel('bed_type', row.bedType) || '-' }}</template>
          </el-table-column>
          <el-table-column label="床位费" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="bedStatusType(row.status)" size="small">
                {{ bedStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前患者" min-width="110">
            <template #default="{ row }">{{ row.patientId ? patientName(row.patientId) : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-perm="'inpatient:bed:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button
                v-if="row.status !== 1 && row.status !== 2"
                v-perm="'inpatient:bed:edit'"
                link
                type="warning"
                @click="onMaintain(row)"
              >
                转维修
              </el-button>
              <el-button
                v-if="row.status === 2"
                v-perm="'inpatient:bed:edit'"
                link
                type="success"
                @click="onRecover(row)"
              >
                维修完成
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改床位' : '新增床位'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="病区" prop="wardName">
              <el-input v-model="form.wardName" placeholder="如 内科一病区" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="床号" prop="bedNo">
              <el-input v-model="form.bedNo" placeholder="如 01-12" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属科室" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="床位类型">
              <el-select v-model="form.bedType" style="width: 100%">
                <el-option label="普通床位" :value="1" />
                <el-option label="重症床位" :value="2" />
                <el-option label="VIP 床位" :value="3" />
                <el-option label="隔离床位" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="床位费">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          v-if="!form.id"
          type="info"
          :closable="false"
          show-icon
          title="新增床位一律以「空闲」入库，占用与维修由住院/维修流程驱动，不在这里手工指定"
        />
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
import { listBeds, maintainBed, pagePatients, recoverBed, saveBed, updateBed } from '@/api/medical'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, loadDeptOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const patientOptions = ref<any[]>([])
const list = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const query = reactive<any>({
  deptId: undefined,
  wardId: undefined,
  status: undefined
})

const form = reactive<any>({
  id: undefined,
  wardId: undefined,
  wardName: '',
  deptId: undefined,
  bedNo: '',
  bedType: 1,
  price: 50,
  status: 0
})

const rules: FormRules = {
  wardName: [{ required: true, message: '请输入病区名称', trigger: 'blur' }],
  bedNo: [{ required: true, message: '请输入床号', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属科室', trigger: 'change' }]
}

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function bedStatusName(status: number) {
  if (status === 1) return '占用'
  if (status === 2) return '维修'
  return '空闲'
}

function bedStatusType(status: number) {
  if (status === 1) return 'warning'
  if (status === 2) return 'danger'
  return 'success'
}

async function onMaintain(row: any) {
  await ElMessageBox.confirm(`确认把 ${row.wardName || ''} ${row.bedNo} 转入维修？转维修后不可安排患者。`, '转维修', {
    type: 'warning'
  })
  await maintainBed(row.id)
  ElMessage.success('已转维修')
  load()
}

async function onRecover(row: any) {
  await ElMessageBox.confirm(`确认 ${row.wardName || ''} ${row.bedNo} 维修完成并恢复使用？`, '维修完成', {
    type: 'warning'
  })
  await recoverBed(row.id)
  ElMessage.success('床位已恢复')
  load()
}

async function load() {
  loading.value = true
  try {
    list.value = ((await listBeds({ ...query })) as any[]) || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.deptId = undefined
  query.wardId = undefined
  query.status = undefined
  load()
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      wardId: undefined,
      wardName: '',
      deptId: query.deptId,
      bedNo: '',
      bedType: 1,
      price: 50,
      status: 0
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
    // 状态不由本表单维护，提交时不带上，避免误改在住床位
    const payload = { ...form, status: undefined, patientId: undefined }
    if (form.id) {
      await updateBed(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await saveBed(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  await useDicts(['bed_type'])
  load()
})
</script>
