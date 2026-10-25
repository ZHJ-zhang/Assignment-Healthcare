<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-slide" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="route.path" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

/**
 * 需要缓存的页面名称集合，取自路由 meta.keepAlive。
 */
const cachedViews = computed<string[]>(() => {
  const names: string[] = []
  route.matched.forEach((item) => {
    const name = item.name
    if (typeof name === 'string' && item.meta?.keepAlive) {
      names.push(name)
    }
  })
  return names
})
</script>

<style scoped lang="scss">
.app-main {
  flex: 1;
  overflow: auto;
  background: var(--hc-bg);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
}
</style>
