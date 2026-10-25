<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">退费管理</h2>
        <p class="page-header__desc">退费申请审核与退费执行跟踪</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="退费单号">
          <el-input
            v-model="query.keyword"
            placeholder="按退费单号搜索"
            clearable
            style="width: 190px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px" @change="search()">
            <el-option label="待审核" :value="0" />
            <el-option label="审核通过" :value="1" />
            <el-option label="已退费" :value="2" />
            <el-option label="审核拒绝" :value="3" />
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
          <el-table-column prop="refundNo" label="退费单号" width="170" />
          <el-table-column label="患者" min-width="120">
            <template #default="{ row }">{{ patientName(row.patientId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="chargeId" label="关联收费单" width="170" />
          <el-table-column label="退费金额" width="120" align="right">
            <template #default="{ row }">
              <span class="amount-danger">￥{{ Number(row.refundAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="退费类型" width="110" align="center">
            <template #default="{ row }">
              {{ row.refundType === 2 ? '全部退费' : '部分退费' }}
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="退费原因" min-width="160" show-overflow-tooltip />
          <el-table-column prop="auditTime" label="审核时间" width="170">
            <template #default="{ row }">{{ row.auditTime || '-' }}</template>
          </el-table-column>
          <el-table-column prop="refundTime" label="退费时间" width="170">
            <template #default="{ row }">{{ row.refundTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">
                {{ statusTag(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'charge:refund'"
                link
                type="success"
                :disabled="row.status !== 0"
                @click="audit(row, true)"
              >
                通过
              </el-button>
              <el-button
                v-perm="'charge:refund'"
                link
                type="danger"
                :disabled="row.status !== 0"
                @click="audit(row, false)"
              >
                驳回
              </el-button>
              <el-button
                v-perm="'charge:refund'"
                link
                type="warning"
                :disabled="row.status !== 1"
                @click="execute(row)"
              >
                执行退费
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
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditRefund, executeRefund, pageRefunds } from '@/api/business'
import { usePage } from '@/composables/usePage'
import { loadPatientOptions, patientName } from '@/utils/lookup'

const { list, total, loading, query, load, search, reset } = usePage(pageRefunds, {
  keyword: '',
  status: undefined
})

function statusTag(status: number) {
  if (status === 0) return { label: '待审核', type: 'warning' as const }
  if (status === 1) return { label: '审核通过', type: 'primary' as const }
  if (status === 2) return { label: '已退费', type: 'success' as const }
  return { label: '审核拒绝', type: 'danger' as const }
}

async function audit(row: any, approved: boolean) {
  const action = approved ? '通过' : '驳回'
  await ElMessageBox.confirm(`确认${action}退费单「${row.refundNo}」吗？`, '提示', { type: 'warning' })
  let reason: string | undefined
  if (!approved) {
    const result = await ElMessageBox.prompt('请填写驳回原因', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '驳回原因',
      inputValidator: (value) => (value && value.trim() ? true : '请输入驳回原因')
    })
    reason = result.value
  }
  await auditRefund(row.id, { approved, reason })
  ElMessage.success(`${action}成功`)
  load()
}

async function execute(row: any) {
  await ElMessageBox.confirm(`确认执行退费「${row.refundNo}」吗？`, '提示', { type: 'warning' })
  await executeRefund(row.id)
  ElMessage.success('退费执行成功')
  load()
}

onMounted(async () => {
  await loadPatientOptions()
  load()
})
</script>

<style scoped>
.amount-danger {
  font-weight: 600;
  color: var(--el-color-danger);
}
</style>
