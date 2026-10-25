<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">参数设置</h2>
        <p class="page-header__desc">维护系统运行参数，业务模块统一从这里取值</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="参数名称 / 参数键"
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
        <el-button v-perm="'system:config:add'" type="primary" :icon="Plus" @click="openDialog()">
          新增参数
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="configName" label="参数名称" min-width="160" />
          <el-table-column prop="configKey" label="参数键" min-width="180" show-overflow-tooltip />
          <el-table-column prop="configValue" label="参数值" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.builtIn === 1" type="warning" size="small">内置</el-tag>
              <el-tag v-else type="info" size="small">自定义</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button v-perm="'system:config:edit'" link type="primary" @click="openDialog(row)">
                修改
              </el-button>
              <el-button
                v-perm="'system:config:remove'"
                link
                type="danger"
                :disabled="row.builtIn === 1"
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load()"
          @size-change="search()"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改参数' : '新增参数'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="参数名称" />
        </el-form-item>
        <el-form-item label="参数键" prop="configKey">
          <el-input v-model="form.configKey" :disabled="form.builtIn === 1" placeholder="唯一键名" />
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="参数值" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { deleteConfig, pageConfigs, saveConfig, updateConfig } from '@/api/system'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageConfigs, { keyword: '' })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  id: undefined,
  configName: '',
  configKey: '',
  configValue: '',
  builtIn: 0,
  remark: ''
})

const rules: FormRules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }]
}

function openDialog(row?: any) {
  Object.assign(form, row || {
    id: undefined,
    configName: '',
    configKey: '',
    configValue: '',
    builtIn: 0,
    remark: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateConfig(form.id, { ...form })
    } else {
      await saveConfig({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除参数「${row.configName}」吗？`, '提示', { type: 'warning' })
  await deleteConfig(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
