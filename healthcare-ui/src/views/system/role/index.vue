<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">角色管理</h2>
        <p class="page-header__desc">定义岗位角色并分配菜单与操作权限</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="角色名称 / 编码"
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
        <el-button v-perm="'system:role:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增角色
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="roleName" label="角色名称" min-width="140" />
          <el-table-column prop="roleCode" label="角色编码" min-width="150" />
          <el-table-column label="数据范围" min-width="130">
            <template #default="{ row }">{{ dataScopeLabel(row.dataScope) }}</template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'system:role:permission'" link type="primary" @click="openPermission(row)">
                分配权限
              </el-button>
              <el-button v-perm="'system:role:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button v-perm="'system:role:remove'" link type="danger" @click="remove(row)">
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load()"
          @size-change="search()"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改角色' : '新增角色'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 doctor" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" :value="1" />
            <el-option label="自定义数据" :value="2" />
            <el-option label="本部门数据" :value="3" />
            <el-option label="本部门及以下数据" :value="4" />
            <el-option label="仅本人数据" :value="5" />
          </el-select>
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
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="分配菜单权限" width="560px">
      <el-alert
        type="info"
        :closable="false"
        title="勾选需要授予该角色的菜单与按钮权限"
        style="margin-bottom: 12px"
      />
      <el-scrollbar max-height="420px">
        <el-tree
          ref="treeRef"
          :data="menuTree"
          show-checkbox
          node-key="id"
          :props="{ label: 'menuName', children: 'children' }"
          :default-checked-keys="checkedKeys"
          :default-expanded-keys="expandedKeys"
        >
          <template #default="{ data }">
            <span class="tree-node">
              {{ data.menuName }}
              <el-tag v-if="data.menuType === 'F'" size="small" type="info" effect="plain">按钮</el-tag>
            </span>
          </template>
        </el-tree>
      </el-scrollbar>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  assignRolePermissions,
  createRole,
  deleteRole,
  getRole,
  pageRoles,
  treeMenus,
  updateRole
} from '@/api/system'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageRoles, { keyword: '' })

const dialogVisible = ref(false)
const permissionVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const treeRef = ref<any>()

const form = reactive<any>({
  id: undefined,
  roleName: '',
  roleCode: '',
  dataScope: 1,
  sort: 0,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const menuTree = ref<any[]>([])
const checkedKeys = ref<number[]>([])
const expandedKeys = ref<number[]>([])
const currentRoleId = ref<number>()

function dataScopeLabel(scope: number) {
  const map: Record<number, string> = {
    1: '全部数据',
    2: '自定义数据',
    3: '本部门数据',
    4: '本部门及以下',
    5: '仅本人数据'
  }
  return map[scope] || '全部数据'
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    roleName: '',
    roleCode: '',
    dataScope: 1,
    sort: 0,
    status: 1,
    remark: ''
  })
}

async function openDialog(row?: any) {
  resetForm()
  if (row) {
    const detail: any = await getRole(row.id)
    Object.assign(form, detail.role || {})
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
      await updateRole(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createRole({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openPermission(row: any) {
  currentRoleId.value = row.id
  if (!menuTree.value.length) {
    menuTree.value = ((await treeMenus()) as any[]) || []
  }
  const detail: any = await getRole(row.id)
  checkedKeys.value = (detail.menuIds || []) as number[]
  expandedKeys.value = menuTree.value.map((item: any) => item.id)
  permissionVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys(checkedKeys.value)
}

async function submitPermission() {
  if (!currentRoleId.value) return
  saving.value = true
  try {
    const ids = [
      ...(treeRef.value?.getCheckedKeys() || []),
      ...(treeRef.value?.getHalfCheckedKeys() || [])
    ].map((item: any) => Number(item))
    await assignRolePermissions(currentRoleId.value, ids)
    ElMessage.success('权限分配成功')
    permissionVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
</style>
