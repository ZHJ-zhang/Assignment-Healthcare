<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">操作审计</h2>
        <p class="page-header__desc">追溯系统操作行为与登录记录</p>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="操作日志" name="operation">
          <el-form :model="query" inline class="search-bar">
            <el-form-item label="业务模块">
              <el-select
                v-model="query.module"
                placeholder="全部模块"
                clearable
                style="width: 170px"
                @change="search()"
              >
                <el-option
                  v-for="item in moduleOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search()">
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
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
              <el-table-column prop="username" label="操作人" width="110" />
              <el-table-column prop="module" label="业务模块" width="130" />
              <el-table-column prop="operation" label="操作描述" min-width="160" show-overflow-tooltip />
              <el-table-column prop="method" label="请求方法" width="150" show-overflow-tooltip />
              <el-table-column prop="ip" label="IP 地址" width="140" />
              <el-table-column prop="costTime" label="耗时(ms)" width="100" align="right" />
              <el-table-column label="结果" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="操作时间" width="170" />
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
        </el-tab-pane>

        <el-tab-pane label="登录日志" name="login">
          <el-form :model="loginQuery" inline class="search-bar">
            <el-form-item label="结果">
              <el-select
                v-model="loginQuery.status"
                placeholder="全部"
                clearable
                style="width: 140px"
                @change="searchLogin()"
              >
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <div class="search-bar__actions">
                <el-button type="primary" :icon="Search" @click="searchLogin()">查询</el-button>
                <el-button :icon="Refresh" @click="resetLogin()">重置</el-button>
                <el-button v-perm="'audit:operation:list'" :icon="Download" @click="exportCsv">
                  导出 CSV
                </el-button>
              </div>
            </el-form-item>
          </el-form>

          <div class="table-wrap">
            <el-table v-loading="loginLoading" :data="loginList" border>
              <el-table-column prop="username" label="用户名" width="120" />
              <el-table-column prop="ip" label="IP 地址" width="140" />
              <el-table-column prop="location" label="登录地点" width="160">
                <template #default="{ row }">{{ row.location || '-' }}</template>
              </el-table-column>
              <el-table-column prop="browser" label="浏览器" width="140">
                <template #default="{ row }">{{ row.browser || '-' }}</template>
              </el-table-column>
              <el-table-column prop="os" label="操作系统" width="130">
                <template #default="{ row }">{{ row.os || '-' }}</template>
              </el-table-column>
              <el-table-column label="结果" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="msg" label="提示信息" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ row.msg || '-' }}</template>
              </el-table-column>
              <el-table-column prop="loginTime" label="登录时间" width="170" />
            </el-table>
          </div>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="loginQuery.pageNum"
              v-model:page-size="loginQuery.pageSize"
              :total="loginTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadLogin()"
              @size-change="loadLogin()"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Download, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportLoginLogs, pageAuditLoginLogs, pageAuditOperationLogs } from '@/api/business'
import { usePage } from '@/composables/usePage'
import { saveBlob } from '@/utils/download'

const activeTab = ref('operation')

const { list, total, loading, query, load, search, reset } = usePage(pageAuditOperationLogs, {
  module: undefined,
  status: undefined
})

const moduleOptions = ref<string[]>([])

const loginList = ref<any[]>([])
const loginTotal = ref(0)
const loginLoading = ref(false)
const loginQuery = reactive<any>({ pageNum: 1, pageSize: 10, status: undefined })

async function loadLogin() {
  loginLoading.value = true
  try {
    const result: any = await pageAuditLoginLogs(loginQuery)
    loginList.value = result?.list || []
    loginTotal.value = Number(result?.total || 0)
  } finally {
    loginLoading.value = false
  }
}

function searchLogin() {
  loginQuery.pageNum = 1
  loadLogin()
}

function resetLogin() {
  loginQuery.status = undefined
  searchLogin()
}

async function exportCsv() {
  try {
    const blob: any = await exportLoginLogs({
      pageNum: 1,
      pageSize: 500,
      status: loginQuery.status
    })
    saveBlob(blob, `登录日志-${Date.now()}.csv`)
  } catch (error: any) {
    ElMessage.error(error?.message || '导出失败')
  }
}

onMounted(async () => {
  const result: any = await pageAuditOperationLogs({ pageNum: 1, pageSize: 500 })
  const modules = new Set<string>()
  ;(result?.list || []).forEach((row: any) => {
    if (row.module) modules.add(row.module)
  })
  moduleOptions.value = Array.from(modules)
  load()
  loadLogin()
})
</script>
