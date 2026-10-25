<template>
  <div class="login-page">
    <div class="login-page__panel">
      <div class="login-page__brand">
        <span class="login-page__mark">HC</span>
        <div>
          <h1 class="login-page__title">医疗后台管理系统</h1>
          <p class="login-page__subtitle">Hospital Information Management System</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        class="login-page__form"
        @keyup.enter="submit"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" clearable>
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-page__submit" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-page__accounts">
        <span class="login-page__accounts-label">演示账号</span>
        <el-tag
          v-for="item in demoAccounts"
          :key="item.username"
          size="small"
          effect="plain"
          class="login-page__account-tag"
          @click="useAccount(item.username)"
        >
          {{ item.label }}
        </el-tag>
        <span class="login-page__accounts-hint">密码统一为 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '超级管理员', username: 'admin' },
  { label: '系统管理员', username: 'system' },
  { label: '门诊医生', username: 'doc01' },
  { label: '护士', username: 'nurse01' },
  { label: '收费员', username: 'cash01' },
  { label: '库管员', username: 'pharma01' },
  { label: '统计员', username: 'report01' }
]

function useAccount(username: string) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    permissionStore.reset()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/index'
    router.push(redirect)
  } catch (error) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at 18% 22%, rgba(15, 138, 126, 0.1), transparent 42%),
    radial-gradient(circle at 82% 78%, rgba(217, 131, 36, 0.09), transparent 46%),
    #eef2f3;
}

.login-page__panel {
  width: 100%;
  max-width: 420px;
  padding: 32px;
  background: var(--hc-surface);
  border: 1px solid var(--hc-border);
  border-radius: 8px;
  box-shadow: 0 12px 40px rgba(22, 38, 43, 0.1);
}

.login-page__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}

.login-page__mark {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  border-radius: var(--hc-radius);
  background: var(--hc-primary);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.login-page__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--hc-text);
}

.login-page__subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--hc-text-muted);
}

.login-page__form :deep(.el-input__wrapper) {
  border-radius: var(--hc-radius);
}

.login-page__submit {
  width: 100%;
  font-size: 15px;
  letter-spacing: 2px;
}

.login-page__accounts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding-top: 18px;
  border-top: 1px dashed var(--hc-border-strong);
}

.login-page__accounts-label {
  font-size: 12px;
  color: var(--hc-text-secondary);
}

.login-page__account-tag {
  cursor: pointer;
}

.login-page__accounts-hint {
  width: 100%;
  margin-top: 2px;
  font-size: 12px;
  color: var(--hc-text-muted);
}
</style>
