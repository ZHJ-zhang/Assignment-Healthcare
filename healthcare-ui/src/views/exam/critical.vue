<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">危急值管理</h2>
        <p class="page-header__desc">跟踪检验检查危急值的通知与处理闭环</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="待处理" :value="0" />
            <el-option label="已通知" :value="1" />
            <el-option label="已处理" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="search()">查询</el-button>
            <el-button :icon="Refresh" @click="reset()">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="itemName" label="检测项目" min-width="150" />
          <el-table-column prop="value" label="检测值" width="130">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.value || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="referenceRange" label="参考范围" width="140">
            <template #default="{ row }">{{ row.referenceRange || '-' }}</template>
          </el-table-column>
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="科室" width="130">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="notifyTime" label="通知时间" width="170" />
          <el-table-column prop="handleTime" label="处理时间" width="170">
            <template #default="{ row }">{{ row.handleTime || '-' }}</template>
          </el-table-column>
          <el-table-column prop="handleRemark" label="处理说明" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.handleRemark || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">
                {{ statusTag(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'exam:critical:handle'"
                link
                type="primary"
                :disabled="row.status === 2"
                @click="openHandle(row)"
              >
                处理
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

    <el-dialog v-model="handleVisible" title="危急值处理" width="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="检测项目">{{ current.itemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="检测值">{{ current.value || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参考范围">{{ current.referenceRange || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ patientName(current.patientId) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="handleForm" label-width="92px" style="margin-top: 16px">
        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="4"
            placeholder="填写接报、处置与通知情况"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { handleCriticalValue, pageCriticalValues } from '@/api/business'
import { usePage } from '@/composables/usePage'
import { deptName, loadDeptOptions, loadPatientOptions, patientName } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageCriticalValues, {
  status: undefined
})

const handleVisible = ref(false)
const saving = ref(false)
const current = reactive<any>({})
const handleForm = reactive<any>({ remark: '' })

function statusTag(value: number) {
  if (value === 0) return { label: '待处理', type: 'danger' as const }
  if (value === 1) return { label: '已通知', type: 'warning' as const }
  return { label: '已处理', type: 'success' as const }
}

function openHandle(row: any) {
  Object.assign(current, row)
  handleForm.remark = row.handleRemark || ''
  handleVisible.value = true
}

async function submitHandle() {
  saving.value = true
  try {
    await handleCriticalValue({ id: current.id, remark: handleForm.remark })
    ElMessage.success('处理成功')
    handleVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadPatientOptions(), loadDeptOptions()])
  load()
})
</script>
