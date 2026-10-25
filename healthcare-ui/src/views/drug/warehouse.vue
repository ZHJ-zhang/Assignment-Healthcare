<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">库房管理</h2>
        <p class="page-header__desc">维护药品库房基本信息与启用状态</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="库房名称 / 编码"
            clearable
            style="width: 220px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="search()">查询</el-button>
            <el-button :icon="Refresh" @click="reset()">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-perm="'drug:warehouse:list'" type="primary" :icon="Plus" @click="openDialog()">
          新增库房
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="filteredList" border>
          <el-table-column prop="warehouseCode" label="库房编码" width="130" />
          <el-table-column prop="warehouseName" label="库房名称" min-width="150" />
          <el-table-column label="所属科室" width="150">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="库房类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.warehouseType === 2 ? 'warning' : 'primary'">
                {{ row.warehouseType === 2 ? '门诊药房' : '院内库房' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="manager" label="负责人" width="110" />
          <el-table-column prop="phone" label="联系电话" width="130" />
          <el-table-column prop="address" label="库房地址" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'drug:warehouse:list'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改库房' : '新增库房'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库房编码" prop="warehouseCode">
              <el-input v-model="form.warehouseCode" placeholder="如 WH001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库房名称" prop="warehouseName">
              <el-input v-model="form.warehouseName" placeholder="库房名称" />
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
          <el-col :span="12">
            <el-form-item label="库房类型">
              <el-select v-model="form.warehouseType" placeholder="请选择" style="width: 100%">
                <el-option label="院内库房" :value="1" />
                <el-option label="门诊药房" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="form.manager" placeholder="负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="库房地址">
              <el-input v-model="form.address" placeholder="详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listWarehouses, saveWarehouse } from '@/api/business'
import { loadDeptOptions, type LookupOption } from '@/utils/lookup'

const list = ref<any[]>([])
const loading = ref(false)
const deptOptions = ref<LookupOption[]>([])
const query = reactive({ keyword: '' })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  warehouseCode: '',
  warehouseName: '',
  deptId: undefined,
  warehouseType: 1,
  manager: '',
  phone: '',
  address: '',
  status: 1
})

const rules: FormRules = {
  warehouseCode: [{ required: true, message: '请输入库房编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入库房名称', trigger: 'blur' }]
}

const filteredList = computed(() => {
  const keyword = query.keyword.trim()
  if (!keyword) return list.value
  return list.value.filter(
    (item) =>
      String(item.warehouseName || '').includes(keyword) ||
      String(item.warehouseCode || '').includes(keyword)
  )
})

function deptName(id: unknown) {
  const hit = deptOptions.value.find((item) => item.value === Number(id))
  return hit ? hit.label : ''
}

async function load() {
  loading.value = true
  try {
    list.value = (await listWarehouses()) || []
  } finally {
    loading.value = false
  }
}

function search() {
  load()
}

function reset() {
  query.keyword = ''
  load()
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      warehouseCode: '',
      warehouseName: '',
      deptId: undefined,
      warehouseType: 1,
      manager: '',
      phone: '',
      address: '',
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
    await saveWarehouse({ ...form })
    ElMessage.success(form.id ? '修改成功' : '新增成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  load()
})
</script>
