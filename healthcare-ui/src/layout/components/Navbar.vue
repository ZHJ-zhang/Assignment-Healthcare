<template>
  <header class="app-navbar">
    <div class="app-navbar__left">
      <el-button text class="app-navbar__collapse" @click="appStore.toggleCollapse()">
        <el-icon :size="18">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="app-navbar__right">
      <el-tooltip content="站内消息" placement="bottom">
        <el-badge :value="unread" :hidden="unread === 0" :max="99">
          <el-button text class="app-navbar__icon-btn" @click="goMessages">
            <el-icon :size="18"><Bell /></el-icon>
          </el-button>
        </el-badge>
      </el-tooltip>

      <el-dropdown trigger="click" @command="onCommand">
        <div class="app-navbar__user">
          <el-avatar :size="30" class="app-navbar__avatar">
            {{ initial }}
          </el-avatar>
          <span class="app-navbar__username">{{ userStore.displayName }}</span>
          <el-icon :size="12"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ userStore.userInfo?.deptName || '未分配科室' }}
            </el-dropdown-item>
            <el-dropdown-item divided command="password">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="88px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { unreadCount } from '@/api/business'
import { changePassword } from '@/api/system'
import { clearDictCache } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const { collapsed } = storeToRefs(appStore)

const unread = ref(0)

const initial = computed(() => (userStore.displayName || 'U').charAt(0))

const breadcrumbs = computed(() => {
  const items: string[] = []
  route.matched.forEach((item) => {
    const title = item.meta?.title
    if (typeof title === 'string' && title) {
      items.push(title)
    }
  })
  return items.length ? items : ['首页']
})

function goMessages() {
  router.push('/message/index')
}

async function onCommand(command: string) {
  if (command === 'password') {
    passwordVisible.value = true
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    permissionStore.reset()
    clearDictCache()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

const passwordVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submitPassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await changePassword({
      userId: userStore.userInfo?.id,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    await userStore.logout()
    permissionStore.reset()
    clearDictCache()
    router.push('/login')
  } finally {
    passwordLoading.value = false
  }
}

onMounted(async () => {
  try {
    unread.value = (await unreadCount()) as unknown as number
  } catch (e) {
    unread.value = 0
  }
})
</script>

<style scoped lang="scss">
.app-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex: 0 0 56px;
  height: 56px;
  padding: 0 16px;
  background: var(--hc-surface);
  border-bottom: 1px solid var(--hc-border);
}

.app-navbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.app-navbar__collapse {
  padding: 6px;
  color: var(--hc-text-secondary);
}

.app-navbar__collapse:hover {
  color: var(--hc-primary);
}

.app-navbar__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-navbar__icon-btn {
  padding: 6px;
  color: var(--hc-text-secondary);
}

.app-navbar__icon-btn:hover {
  color: var(--hc-primary);
}

.app-navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: var(--hc-radius-sm);
  cursor: pointer;
  outline: none;
}

.app-navbar__user:hover {
  background: var(--hc-primary-light);
}

.app-navbar__avatar {
  background: var(--hc-primary);
  color: #fff;
  font-size: 13px;
}

.app-navbar__username {
  max-width: 120px;
  overflow: hidden;
  font-size: 14px;
  color: var(--hc-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .app-navbar__username {
    display: none;
  }
}
</style>
