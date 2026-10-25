<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">收费项目管理</h2>
        <p class="page-header__desc">维护诊疗、药品、检查等收费项目的价格与医保属性</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="项目名称 / 编码"
            clearable
            style="width: 220px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="query.itemType" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="药品" :value="1" />
            <el-option label="诊疗" :value="2" />
            <el-option label="检查" :value="3" />
            <el-option label="床位" :value="4" />
            <el-option label="护理" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search()">
            <el-option label="启用" :value="1" />
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
        <el-button v-perm="'charge:feeItem:edit'" type="primary" :icon="Plus" @click="openDialog()">
          新增收费项目
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="itemCode" label="项目编码" width="140" />
          <el-table-column prop="itemName" label="项目名称" min-width="180" />
          <el-table-column label="项目类型" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="itemTypeTag(row.itemType).type">
                {{ itemTypeTag(row.itemType).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="90" align="center">
            <template #default="{ row }">{{ row.unit || '-' }}</template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">￥{{ Number(row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="医保项目" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.medicalInsurance === 1" type="success" size="small">医保</el-tag>
              <span v-else class="text-muted">自费</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'charge:feeItem:edit'" link type="primary" @click="openDialog(row)">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改收费项目' : '新增收费项目'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目编码" prop="itemCode">
              <el-input v-model="form.itemCode" placeholder="如 FY0001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="项目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型" prop="itemType">
              <el-select v-model="form.itemType" placeholder="请选择" style="width: 100%">
                <el-option label="药品" :value="1" />
                <el-option label="诊疗" :value="2" />
                <el-option label="检查" :value="3" />
                <el-option label="床位" :value="4" />
                <el-option label="护理" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="次 / 天 / 支" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医保项目">
              <el-radio-group v-model="form.medicalInsurance">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
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
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createFeeItem, pageFeeItems, updateFeeItem } from '@/api/business'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageFeeItems, {
  keyword: '',
  itemType: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  itemCode: '',
  itemName: '',
  itemType: 2,
  unit: '次',
  price: 0,
  medicalInsurance: 1,
  status: 1
})

const rules: FormRules = {
  itemCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

function itemTypeTag(type: number) {
  const map: Record<number, { label: string; type: any }> = {
    1: { label: '药品', type: 'success' },
    2: { label: '诊疗', type: 'primary' },
    3: { label: '检查', type: 'warning' },
    4: { label: '床位', type: 'info' },
    5: { label: '护理', type: 'info' }
  }
  return map[type] ?? { label: '其他', type: 'info' }
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      itemCode: '',
      itemName: '',
      itemType: 2,
      unit: '次',
      price: 0,
      medicalInsurance: 1,
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
    if (form.id) {
      await updateFeeItem(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createFeeItem({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
