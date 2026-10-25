<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">站内消息</h2>
        <p class="page-header__desc">系统通知、待办提醒与预警消息</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="阅读状态">
          <el-select v-model="query.isRead" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
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
        <el-button v-perm="'message:read'" type="primary" :icon="Select" @click="markAll">全部标记已读</el-button>
        <span class="form-tip">当前未读 {{ unread }} 条</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column label="标题" min-width="200">
            <template #default="{ row }">
              <span :class="{ 'msg-unread': row.isRead === 0 }">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
          <el-table-column label="消息类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="msgTypeTag(row.msgType).type" size="small">
                {{ msgTypeTag(row.msgType).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizId" label="业务编号" width="150">
            <template #default="{ row }">{{ row.bizId || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="发送时间" width="170" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" size="small">
                {{ row.isRead === 1 ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-perm="'message:read'"
                link
                type="primary"
                :disabled="row.isRead === 1"
                @click="markRead(row)"
              >
                标记已读
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
import { onMounted, ref } from 'vue'
import { Refresh, Search, Select } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMessages, readAllMessages, readMessage, unreadCount } from '@/api/business'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageMessages, {
  isRead: undefined
})

const unread = ref(0)

function msgTypeTag(type: number) {
  if (type === 1) return { label: '系统通知', type: 'primary' as const }
  if (type === 2) return { label: '待办提醒', type: 'warning' as const }
  return { label: '预警提醒', type: 'danger' as const }
}

async function loadUnread() {
  unread.value = Number((await unreadCount()) || 0)
}

async function markRead(row: any) {
  await readMessage(row.id)
  ElMessage.success('已标记为已读')
  load()
  loadUnread()
}

async function markAll() {
  await ElMessageBox.confirm('确认将全部消息标记为已读吗？', '提示', { type: 'warning' })
  const count = await readAllMessages()
  ElMessage.success(`已标记 ${count || 0} 条消息`)
  load()
  loadUnread()
}

onMounted(async () => {
  load()
  loadUnread()
})
</script>

<style scoped>
.msg-unread {
  font-weight: 600;
  color: var(--hc-primary);
}
</style>
