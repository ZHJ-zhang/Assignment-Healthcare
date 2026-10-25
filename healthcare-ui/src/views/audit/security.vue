<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">安全策略</h2>
        <p class="page-header__desc">配置账号锁定、会话超时、敏感信息脱敏等安全规则</p>
      </div>
    </div>

    <div class="content-card">
      <el-form v-loading="loading" :model="form" label-width="160px" style="max-width: 720px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录失败锁定次数">
              <el-input-number v-model="form.loginFailLimit" :min="0" :max="20" :precision="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号锁定时长（分钟）">
              <el-input-number v-model="form.lockMinutes" :min="1" :max="1440" :precision="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会话超时（分钟）">
              <el-input-number v-model="form.sessionTimeout" :min="5" :max="1440" :precision="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码有效期（天）">
              <el-input-number v-model="form.passwordExpireDays" :min="0" :max="999" :precision="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码最小长度">
              <el-input-number v-model="form.passwordMinLength" :min="4" :max="32" :precision="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="双因素认证">
              <el-switch v-model="form.twoFactorEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="敏感信息脱敏">
              <el-switch v-model="form.maskEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="IP 白名单">
              <el-input
                v-model="form.ipWhitelist"
                type="textarea"
                :rows="3"
                placeholder="多个 IP 用英文逗号分隔，留空表示不限制"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button v-perm="'audit:security:edit'" type="primary" :loading="saving" @click="submit">
            保存策略
          </el-button>
          <el-button :icon="Refresh" @click="load">重新加载</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <div class="content-card__header">
        <h3 class="content-card__title">策略说明</h3>
      </div>
      <ul class="policy-list">
        <li>登录失败达到锁定次数后，账号将被锁定，锁定期间无法登录。</li>
        <li>会话超时后需要重新登录以获取新的访问令牌。</li>
        <li>开启敏感信息脱敏后，列表中的身份证号与手机号按规则隐藏。</li>
        <li>密码有效期到期后，用户需修改密码才能继续使用系统。</li>
        <li>IP 白名单中的地址不受限制，留空表示允许所有来源访问。</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSecurityConfig, saveSecurityConfig } from '@/api/business'

const loading = ref(false)
const saving = ref(false)

const form = reactive<any>({
  loginFailLimit: 5,
  lockMinutes: 30,
  sessionTimeout: 120,
  ipWhitelist: '',
  twoFactorEnabled: 0,
  maskEnabled: 1,
  passwordMinLength: 6,
  passwordExpireDays: 90
})

async function load() {
  loading.value = true
  try {
    const result: any = await getSecurityConfig()
    Object.assign(form, result || {})
  } finally {
    loading.value = false
  }
}

async function submit() {
  saving.value = true
  try {
    await saveSecurityConfig({ ...form })
    ElMessage.success('安全策略已保存')
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.policy-list {
  margin: 0;
  padding-left: 20px;
  color: var(--hc-text-muted);
  font-size: 13px;
  line-height: 2;
}
</style>
