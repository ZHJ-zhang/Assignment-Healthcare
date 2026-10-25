<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">菜单管理</h2>
        <p class="page-header__desc">配置左侧导航结构与按钮级权限标识</p>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-button v-perm="'system:menu:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增菜单
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="list"
          row-key="id"
          border
          default-expand-all
          :tree-props="{ children: 'children' }"
        >
          <el-table-column prop="menuName" label="菜单名称" min-width="180" />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="row.menuType === 'M' ? 'warning' : row.menuType === 'C' ? 'success' : 'info'"
              >
                {{ row.menuType === 'M' ? '目录' : row.menuType === 'C' ? '菜单' : '按钮' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="icon" label="图标" width="90" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路由地址" min-width="140" />
          <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
          <el-table-column prop="perms" label="权限标识" min-width="170" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'system:menu:add'" link type="primary" @click="openDialog(null, row)">
                添加
              </el-button>
              <el-button v-perm="'system:menu:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button v-perm="'system:menu:remove'" link type="danger" @click="remove(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改菜单' : '新增菜单'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="list"
            :props="{ label: 'menuName', children: 'children', value: 'id' }"
            check-strictly
            clearable
            placeholder="不选则为顶级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="显示在导航中的名称" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 User" />
        </el-form-item>
        <el-form-item label="路由地址">
          <el-input v-model="form.path" placeholder="如 user" />
        </el-form-item>
        <el-form-item label="组件路径">
          <el-input v-model="form.component" placeholder="相对 src/views，如 system/user/index" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="显示">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
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
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createMenu, deleteMenu, getMenu, treeMenus, updateMenu } from '@/api/system'

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  parentId: 0,
  menuName: '',
  menuType: 'C',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1
})

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    list.value = ((await treeMenus()) as any[]) || []
  } finally {
    loading.value = false
  }
}

async function openDialog(row?: any, parent?: any) {
  if (row) {
    const detail: any = await getMenu(row.id)
    Object.assign(form, detail || {})
  } else {
    Object.assign(form, {
      id: undefined,
      parentId: parent ? parent.id : 0,
      menuName: '',
      menuType: parent ? 'C' : 'M',
      path: '',
      component: '',
      perms: '',
      icon: '',
      sort: 0,
      visible: 1,
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
      await updateMenu(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createMenu({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
