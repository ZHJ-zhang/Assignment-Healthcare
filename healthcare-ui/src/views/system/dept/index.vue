<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">科室管理</h2>
        <p class="page-header__desc">维护医院组织架构与科室信息</p>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-button v-perm="'system:dept:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增科室
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" row-key="id" border default-expand-all>
          <el-table-column prop="deptName" label="科室名称" min-width="200" />
          <el-table-column prop="deptCode" label="科室编码" min-width="130" />
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">{{ dictLabel('dept_type', row.deptType) }}</template>
          </el-table-column>
          <el-table-column prop="leader" label="负责人" width="110" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'system:dept:add'" link type="primary" @click="openDialog(null, row)">
                添加下级
              </el-button>
              <el-button v-perm="'system:dept:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button v-perm="'system:dept:remove'" link type="danger" @click="remove(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改科室' : '新增科室'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="上级科室">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', children: 'children', value: 'id' }"
            check-strictly
            clearable
            placeholder="不选则为顶级科室"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="科室名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="科室名称" />
        </el-form-item>
        <el-form-item label="科室编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="科室类型">
          <el-select v-model="form.deptType" style="width: 100%">
            <el-option label="临床科室" :value="1" />
            <el-option label="医技科室" :value="2" />
            <el-option label="行政科室" :value="3" />
            <el-option label="后勤科室" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="科室负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createDept, deleteDept, treeDepts, updateDept } from '@/api/system'
import { dictLabel, useDicts } from '@/utils/dict'

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  parentId: 0,
  deptName: '',
  deptCode: '',
  deptType: 1,
  leader: '',
  phone: '',
  sort: 0,
  status: 1
})

const rules: FormRules = {
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }]
}

/**
 * 上级科室下拉数据，顶级科室的 parentId 归零。
 */
const parentOptions = computed(() => list.value)

async function load() {
  loading.value = true
  try {
    list.value = ((await treeDepts()) as any[]) || []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any, parent?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      parentId: parent ? parent.id : 0,
      deptName: '',
      deptCode: '',
      deptType: 1,
      leader: '',
      phone: '',
      sort: 0,
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
      await updateDept(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createDept({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除科室「${row.deptName}」吗？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  await useDicts(['dept_type'])
  load()
})
</script>
