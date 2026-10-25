<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">数据备份</h2>
        <p class="page-header__desc">创建数据备份、恢复与历史备份管理</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px" @change="search()">
            <el-option label="备份完成" :value="1" />
            <el-option label="已恢复" :value="2" />
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
        <el-button v-perm="'audit:backup:add'" type="primary" :icon="FolderAdd" @click="openDialog()">
          创建备份
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="backupName" label="备份名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="备份类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.backupType === 2 ? 'warning' : 'primary'">
                {{ row.backupType === 2 ? '增量备份' : '全量备份' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="backupPath" label="存储位置" min-width="280" show-overflow-tooltip />
          <el-table-column label="备份大小" width="120" align="right">
            <template #default="{ row }">{{ formatSize(row.backupSize) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="备份时间" width="170" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                {{ row.status === 1 ? '备份完成' : '已恢复' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'audit:backup:restore'"
                link
                type="warning"
                :disabled="row.status === 2"
                @click="restore(row)"
              >
                恢复
              </el-button>
              <el-button v-perm="'audit:backup:remove'" link type="danger" @click="remove(row)">
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

    <el-dialog v-model="dialogVisible" title="创建数据备份" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="备份名称">
          <el-input v-model="form.backupName" placeholder="留空则自动生成" />
        </el-form-item>
        <el-form-item label="备份类型" prop="backupType">
          <el-radio-group v-model="form.backupType">
            <el-radio :value="1">全量备份</el-radio>
            <el-radio :value="2">增量备份</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">开始备份</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { FolderAdd, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createBackup, deleteBackup, pageBackups, restoreBackup } from '@/api/business'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageBackups, {
  status: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  backupName: '',
  backupType: 1
})

const rules: FormRules = {
  backupType: [{ required: true, message: '请选择备份类型', trigger: 'change' }]
}

function formatSize(bytes: unknown) {
  const num = Number(bytes || 0)
  if (num < 1024) return `${num} B`
  if (num < 1024 * 1024) return `${(num / 1024).toFixed(1)} KB`
  if (num < 1024 * 1024 * 1024) return `${(num / 1024 / 1024).toFixed(1)} MB`
  return `${(num / 1024 / 1024 / 1024).toFixed(2)} GB`
}

function openDialog() {
  Object.assign(form, { backupName: '', backupType: 1 })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createBackup({ ...form })
    ElMessage.success('备份创建成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function restore(row: any) {
  await ElMessageBox.confirm(
    `确认恢复备份「${row.backupName}」吗？恢复操作将由 DBA 依据备份文件执行。`,
    '提示',
    { type: 'warning' }
  )
  await restoreBackup(row.id)
  ElMessage.success('备份已标记为已恢复')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除备份记录「${row.backupName}」吗？`, '提示', { type: 'warning' })
  await deleteBackup(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
