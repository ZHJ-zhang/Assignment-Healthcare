<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">门诊病历</h2>
        <p class="page-header__desc">归档门诊病历并跟踪书写状态</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="就诊号 / 诊断"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="患者">
          <el-select v-model="query.patientId" filterable placeholder="全部" clearable style="width: 210px">
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name}（${item.patientNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="草稿" :value="0" />
            <el-option label="已提交" :value="1" />
            <el-option label="已归档" :value="2" />
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
          <el-table-column prop="visitNo" label="就诊号" min-width="140" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) }}</template>
          </el-table-column>
          <el-table-column label="就诊类型" width="110" align="center">
            <template #default="{ row }">{{ dictLabel('visit_type', row.visitType) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="chiefComplaint" label="主诉" min-width="170" show-overflow-tooltip />
          <el-table-column prop="diagnosis" label="诊断" min-width="150" show-overflow-tooltip />
          <el-table-column label="科室" min-width="120">
            <template #default="{ row }">{{ deptName(row.deptId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="医生" width="100">
            <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="visitTime" label="就诊时间" min-width="170" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="recordStatusType(row.status)" size="small">
                {{ recordStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">查看</el-button>
              <el-button
                v-if="row.status === 0"
                v-perm="'clinic:record:save'"
                link
                type="primary"
                @click="submitRecord(row)"
              >
                提交
              </el-button>
              <el-button
                v-if="row.status === 1"
                v-perm="'clinic:record:save'"
                link
                type="success"
                @click="archiveRecordRow(row)"
              >
                归档
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

    <el-drawer v-model="detailVisible" title="门诊病历" size="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="就诊号">{{ current.visitNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ patientName(current.patientId) }}</el-descriptions-item>
        <el-descriptions-item label="就诊类型">
          {{ dictLabel('visit_type', current.visitType) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="主诉">{{ current.chiefComplaint || '无' }}</el-descriptions-item>
        <el-descriptions-item label="现病史">{{ current.presentIllness || '无' }}</el-descriptions-item>
        <el-descriptions-item label="体格检查">{{ current.physicalExam || '无' }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ current.diagnosis || '无' }}</el-descriptions-item>
        <el-descriptions-item label="ICD 编码">{{ current.icdCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">{{ current.visitTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageClinicRecords, pagePatients, submitClinicRecord, archiveClinicRecord } from '@/api/medical'
import { usePage } from '@/composables/usePage'
import { dictLabel, useDicts } from '@/utils/dict'
import { deptName, doctorName, loadDeptOptions, loadDoctorOptions } from '@/utils/lookup'

const patientOptions = ref<any[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageClinicRecords, {
  keyword: '',
  patientId: undefined,
  status: undefined
})

const detailVisible = ref(false)
const current = ref<any>({})

function patientName(id: number) {
  const hit = patientOptions.value.find((item) => item.id === id)
  return hit ? hit.name : id
}

function showDetail(row: any) {
  current.value = row
  detailVisible.value = true
}

function recordStatusName(status: number) {
  if (status === 1) return '已提交'
  if (status === 2) return '已归档'
  return '草稿'
}

function recordStatusType(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

async function submitRecord(row: any) {
  await ElMessageBox.confirm('确认提交该病历吗？提交后仍可继续修改，归档后才会锁死。', '提示', { type: 'warning' })
  await submitClinicRecord(row.id)
  ElMessage.success('提交成功')
  load()
}

async function archiveRecordRow(row: any) {
  await ElMessageBox.confirm('确认归档该病历吗？归档后不可再修改，用于病案室留档。', '归档病历', { type: 'warning' })
  await archiveClinicRecord(row.id)
  ElMessage.success('归档成功')
  load()
}

onMounted(async () => {
  const result: any = await pagePatients({ pageNum: 1, pageSize: 500 })
  patientOptions.value = result?.list || []
  await Promise.all([loadDeptOptions(), loadDoctorOptions()])
  await useDicts(['visit_type'])
  load()
})
</script>
