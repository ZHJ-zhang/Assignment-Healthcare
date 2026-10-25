<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">用户管理</h2>
        <p class="page-header__desc">维护系统账号、所属科室与角色权限</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="账号 / 姓名 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="所属科室">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
        <div class="toolbar__left">
          <el-button v-perm="'system:user:add'" type="primary" :icon="Plus" @click="openDialog()">
            新增用户
          </el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="username" label="账号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column label="性别" width="70" align="center">
            <template #default="{ row }">{{ dictLabel('sys_gender', row.gender) }}</template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="130" />
          <el-table-column label="所属科室" min-width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'system:user:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button
                v-perm="'system:user:status'"
                link
                type="primary"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button v-perm="'system:user:resetPwd'" link type="warning" @click="resetPwd(row)">
                重置密码
              </el-button>
              <el-button
                v-perm="'system:user:remove'"
                link
                type="danger"
                @click="remove(row)"
              >
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load()"
          @size-change="search()"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改用户' : '新增用户'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="rawPassword">
              <el-input
                v-model="form.rawPassword"
                type="password"
                show-password
                :placeholder="form.id ? '留空表示不修改' : '初始密码'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属科室">
              <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="电子邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
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
import {
  createUser,
  deleteUser,
  getUser,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/api/system'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, loadDeptOptions, loadRoleOptions, type LookupOption } from '@/utils/lookup'

const deptOptions = ref<LookupOption[]>([])
const roleOptions = ref<LookupOption[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageUsers, {
  keyword: '',
  deptId: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  username: '',
  realName: '',
  rawPassword: '',
  gender: 1,
  phone: '',
  email: '',
  deptId: undefined,
  status: 1,
  remark: '',
  roleIds: []
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  rawPassword: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (!form.id && !value) {
          callback(new Error('请输入初始密码'))
        } else if (value && value.length < 6) {
          callback(new Error('密码长度不能少于 6 位'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请至少选择一个角色', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    rawPassword: '',
    gender: 1,
    phone: '',
    email: '',
    deptId: undefined,
    status: 1,
    remark: '',
    roleIds: []
  })
}

async function openDialog(row?: any) {
  resetForm()
  if (row) {
    const detail: any = await getUser(row.id)
    Object.assign(form, detail.user || {})
    form.roleIds = detail.roleIds || []
    form.rawPassword = ''
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (!payload.rawPassword) delete payload.rawPassword
    if (form.id) {
      await updateUser(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createUser(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: any) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定${target === 1 ? '启用' : '停用'}账号「${row.realName}」吗？`,
    '提示',
    { type: 'warning' }
  )
  await updateUserStatus(row.id, target)
  ElMessage.success('操作成功')
  load()
}

async function resetPwd(row: any) {
  const { value } = await ElMessageBox.prompt('请输入新的初始密码', '重置密码', {
    inputValue: '123456',
    inputPattern: /.{6,}/,
    inputErrorMessage: '密码长度不能少于 6 位'
  })
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除用户「${row.realName}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  roleOptions.value = await loadRoleOptions()
  await useDicts(['sys_gender'])
  load()
})
</script>
