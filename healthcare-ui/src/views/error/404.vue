<template>
  <div class="not-found">
    <div class="not-found__code">404</div>
    <p class="not-found__text">抱歉，您访问的页面不存在或已被移除。</p>
    <el-button type="primary" @click="router.push('/index')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--hc-bg);
}

.not-found__code {
  font-size: 84px;
  font-weight: 700;
  line-height: 1;
  color: var(--hc-primary);
  letter-spacing: 4px;
}

.not-found__text {
  margin: 16px 0 24px;
  color: var(--hc-text-secondary);
}
</style>
