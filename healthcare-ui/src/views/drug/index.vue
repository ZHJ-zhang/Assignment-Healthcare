<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">药品目录</h2>
        <p class="page-header__desc">维护医院药品基础信息与价格</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="药品名称 / 编码 / 厂家"
            clearable
            style="width: 220px"
            @keyup.enter="search()"
            @clear="search()"
          />
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
        <el-button v-perm="'drug:add'" type="primary" :icon="Plus" @click="openDialog()">新增药品</el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="drugCode" label="药品编码" min-width="130" />
          <el-table-column prop="drugName" label="药品名称" min-width="140" />
          <el-table-column prop="genericName" label="通用名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="spec" label="规格" width="120" />
          <el-table-column prop="dosageForm" label="剂型" width="100" />
          <el-table-column prop="unit" label="单位" width="80" align="center" />
          <el-table-column label="进价" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.purchasePrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="售价" width="100" align="right">
            <template #default="{ row }">￥{{ Number(row.salePrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="特殊药品" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isSpecial === 1" type="danger" size="small">管制</el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'drug:edit'" link type="primary" @click="openDialog(row)">修改</el-button>
              <el-button v-perm="'drug:remove'" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改药品' : '新增药品'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="药品编码" prop="drugCode">
              <el-input v-model="form.drugCode" placeholder="如 YP0001" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="药品名称" prop="drugName">
              <el-input v-model="form.drugName" placeholder="商品名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="通用名称">
              <el-input v-model="form.genericName" placeholder="通用名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="form.spec" placeholder="如 0.25g*24片" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="剂型">
              <el-select v-model="form.dosageForm" placeholder="请选择" clearable style="width: 100%">
                <el-option label="片剂" value="片剂" />
                <el-option label="胶囊" value="胶囊" />
                <el-option label="颗粒剂" value="颗粒剂" />
                <el-option label="注射剂" value="注射剂" />
                <el-option label="口服液" value="口服液" />
                <el-option label="软膏" value="软膏" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="盒 / 支 / 瓶" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产厂家">
              <el-input v-model="form.manufacturer" placeholder="生产企业" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批准文号">
              <el-input v-model="form.approvalNo" placeholder="国药准字" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="如 西药" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="进价">
              <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="特殊药品">
              <el-radio-group v-model="form.isSpecial">
                <el-radio :value="0">否</el-radio>
                <el-radio :value="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createDrug, deleteDrug, getDrug, pageDrugs, updateDrug } from '@/api/business'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageDrugs, {
  keyword: '',
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  drugCode: '',
  drugName: '',
  genericName: '',
  spec: '',
  dosageForm: '',
  unit: '',
  manufacturer: '',
  approvalNo: '',
  purchasePrice: 0,
  salePrice: 0,
  category: '',
  isSpecial: 0,
  status: 1
})

const rules: FormRules = {
  drugCode: [{ required: true, message: '请输入药品编码', trigger: 'blur' }],
  drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  spec: [{ required: true, message: '请输入规格', trigger: 'blur' }]
}

async function openDialog(row?: any) {
  if (row) {
    const detail: any = await getDrug(row.id)
    Object.assign(form, detail || {})
  } else {
    Object.assign(form, {
      id: undefined,
      drugCode: '',
      drugName: '',
      genericName: '',
      spec: '',
      dosageForm: '',
      unit: '',
      manufacturer: '',
      approvalNo: '',
      purchasePrice: 0,
      salePrice: 0,
      category: '',
      isSpecial: 0,
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
      await updateDrug(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createDrug({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除药品「${row.drugName}」吗？`, '提示', { type: 'warning' })
  await deleteDrug(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
