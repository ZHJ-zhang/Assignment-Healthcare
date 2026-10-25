<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">短信通知</h2>
        <p class="page-header__desc">发送短信通知并查询发送记录</p>
      </div>
    </div>

    <div class="content-card">
      <el-form :model="query" inline class="search-bar">
        <el-form-item label="发送状态">
          <el-select v-model="query.sendStatus" placeholder="全部" clearable style="width: 140px" @change="search()">
            <el-option label="发送中" :value="0" />
            <el-option label="发送成功" :value="1" />
            <el-option label="发送失败" :value="2" />
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
        <el-button v-perm="'message:sms:send'" type="primary" :icon="ChatDotRound" @click="openDialog()">
          发送短信
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="templateCode" label="模板编码" width="150">
            <template #default="{ row }">{{ row.templateCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="content" label="短信内容" min-width="280" show-overflow-tooltip />
          <el-table-column label="发送状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="sendStatusTag(row.sendStatus).type" size="small">
                {{ sendStatusTag(row.sendStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMsg" label="失败原因" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.errorMsg || '-' }}</template>
          </el-table-column>
          <el-table-column prop="sendTime" label="发送时间" width="170" />
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

    <el-dialog v-model="dialogVisible" title="发送短信" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="接收方手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-select v-model="form.templateCode" placeholder="可选" clearable style="width: 100%">
            <el-option label="预约提醒" value="APPOINTMENT_REMIND" />
            <el-option label="报告通知" value="REPORT_READY" />
            <el-option label="随访提醒" value="FOLLOW_UP_REMIND" />
            <el-option label="缴费通知" value="PAYMENT_NOTICE" />
            <el-option label="接种提醒" value="VACCINATION_REMIND" />
          </el-select>
        </el-form-item>
        <el-form-item label="短信内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入短信内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ChatDotRound, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pageSmsRecords, sendSms } from '@/api/business'
import { usePage } from '@/composables/usePage'

const { list, total, loading, query, load, search, reset } = usePage(pageSmsRecords, {
  sendStatus: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<any>({
  phone: '',
  templateCode: '',
  content: ''
})

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入短信内容', trigger: 'blur' }]
}

function sendStatusTag(status: number) {
  if (status === 0) return { label: '发送中', type: 'warning' as const }
  if (status === 1) return { label: '发送成功', type: 'success' as const }
  return { label: '发送失败', type: 'danger' as const }
}

function openDialog() {
  Object.assign(form, { phone: '', templateCode: '', content: '' })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await sendSms({
      phone: form.phone,
      templateCode: form.templateCode,
      content: form.content
    })
    ElMessage.success('短信已提交发送')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
