<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">登录日志</h2>
        <p class="page-header__desc">追溯账号的登录来源与成败记录</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="账号">
          <el-input
            v-model="query.keyword"
            placeholder="登录账号"
            clearable
            style="width: 200px"
            @keyup.enter="search()"
            @clear="search()"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
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
          <el-table-column prop="username" label="账号" min-width="120" />
          <el-table-column prop="ip" label="IP 地址" min-width="140" />
          <el-table-column prop="location" label="登录地点" min-width="120" />
          <el-table-column prop="browser" label="浏览器" min-width="110" />
          <el-table-column prop="os" label="操作系统" min-width="120" />
          <el-table-column prop="msg" label="提示信息" min-width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="loginTime" label="登录时间" width="170" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { pageLoginLogs } from '@/api/system'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageLoginLogs, {
  keyword: '',
  status: undefined
})

onMounted(load)
</script>
