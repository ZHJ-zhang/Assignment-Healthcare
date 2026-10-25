<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">操作日志</h2>
        <p class="page-header__desc">记录系统内所有写操作的执行轨迹</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="操作人 / 操作内容"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="业务模块" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <div class="search-bar__actions">
            <el-button type="primary" :icon="Search" @click="doSearch()">查询</el-button>
            <el-button :icon="Refresh" @click="doReset()">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="username" label="操作人" width="110" />
          <el-table-column prop="module" label="模块" width="130" />
          <el-table-column prop="operation" label="操作内容" min-width="150" />
          <el-table-column prop="method" label="请求方式" width="160" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP 地址" width="140" />
          <el-table-column label="耗时" width="100" align="right">
            <template #default="{ row }">{{ row.costTime ?? 0 }} ms</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="操作时间" width="170" />
          <el-table-column label="详情" width="90" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">查看</el-button>
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
          @size-change="doSearch()"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="操作详情" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作人">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="detail-pre">{{ detail.params || '无' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="客户端">{{ detail.userAgent || '未知' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageOperationLogs } from '@/api/system'
import { usePage } from '@/composables/usePage'

const dateRange = ref<string[]>([])

const { list, total, loading, query, load, search, reset } = usePage(pageOperationLogs, {
  keyword: '',
  module: '',
  status: undefined,
  startTime: '',
  endTime: ''
})

const detailVisible = ref(false)
const detail = ref<any>({})

function showDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

function applyRange() {
  query.startTime = dateRange.value?.[0] || ''
  query.endTime = dateRange.value?.[1] || ''
}

function doSearch() {
  applyRange()
  return search()
}

function doReset() {
  dateRange.value = []
  return reset()
}

onMounted(load)
</script>

<style scoped lang="scss">
.detail-pre {
  max-height: 220px;
  margin: 0;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
