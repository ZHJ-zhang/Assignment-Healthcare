<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">公告管理</h2>
        <p class="page-header__desc">发布院内公告与科室公告</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="公告类型">
          <el-select v-model="query.noticeType" placeholder="全部" clearable style="width: 150px" @change="search()">
            <el-option label="院内公告" :value="1" />
            <el-option label="科室公告" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="已发布" :value="1" />
            <el-option label="已撤回" :value="0" />
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
        <el-button v-perm="'message:notice:add'" type="primary" :icon="Plus" @click="openDialog()">
          发布公告
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="title" label="公告标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.noticeType === 2 ? 'warning' : 'primary'" size="small">
                {{ row.noticeType === 2 ? '科室公告' : '院内公告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布科室" width="150">
            <template #default="{ row }">{{ deptName(row.deptId) || '全院' }}</template>
          </el-table-column>
          <el-table-column prop="content" label="公告内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="publishTime" label="发布时间" width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '已发布' : '已撤回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'message:notice:remove'" link type="danger" @click="remove(row)">
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

    <el-dialog v-model="dialogVisible" title="发布公告" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="公告类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio :value="1">院内公告</el-radio>
            <el-radio :value="2">科室公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发布科室">
          <el-select v-model="form.deptId" placeholder="全院" clearable style="width: 100%">
            <el-option
              v-for="item in deptOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="公告正文"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { deleteNotice, pageNotices, publishNotice } from '@/api/business'
import { usePage } from '@/composables/usePage'
import { deptName, loadDeptOptions, type LookupOption } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageNotices, {
  noticeType: undefined,
  status: undefined
})

const deptOptions = ref<LookupOption[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  title: '',
  content: '',
  noticeType: 1,
  deptId: undefined,
  status: 1
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
}

function openDialog() {
  Object.assign(form, {
    title: '',
    content: '',
    noticeType: 1,
    deptId: undefined,
    status: 1
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await publishNotice({ ...form })
    ElMessage.success('公告发布成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  deptOptions.value = await loadDeptOptions()
  load()
})
</script>
